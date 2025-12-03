// WorkoutViewModel.kt
package com.ptit.iot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptit.iot.CustomException
import com.ptit.iot.HeartRateResponse
import com.ptit.iot.ProfileResponse
import com.ptit.iot.RemoteModule
import com.ptit.iot.Resource
import com.ptit.iot.UnknownException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

data class WorkoutStats(
    val duration: Long = 0, // ms
    val bpmReadings: List<Double> = emptyList(),
    val avgBpm: Double = 0.0,
    val maxBpm: Double = 0.0,
    val minBpm: Double = 0.0,
    val caloriesBurned: Double = 0.0,
    val stepCount: Int = 0,
    val targetDistance: Double = 0.0, // km
    val currentDistance: Double = 0.0, // km
    val isWorkoutCompleted: Boolean = false
)

class WorkoutViewModel : ViewModel() {
    private val _heartState = Channel<Resource<HeartRateResponse>>()
    // biến lớp (nếu chưa có, thêm vào ViewModel)
    private var lastStepTimestamp: Long = 0L
    private val STEP_DELAY_MS: Long = 300L     // phút giữa 2 bước (tune: 200..400)
    private val STEP_THRESHOLD: Double = 10.5  // ngưỡng (tune: 9.5..11.0)

    // tuỳ chọn smoothing để giảm nhiễu (bật nếu dữ liệu noisy)
    private val USE_SMOOTHING = true
    private val SMOOTHING_ALPHA = 0.2          // 0..1, nhỏ hơn = mượt hơn
    private var lastFilteredMagnitude: Double = 0.0
    val heartState = _heartState.receiveAsFlow()

    private val _profileState = Channel<Resource<ProfileResponse.Data>>()
    val profileState = _profileState.receiveAsFlow()

    private val _workoutStats = MutableStateFlow<WorkoutStats>(WorkoutStats())
    val workoutStats = _workoutStats.asStateFlow()

    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive = _isWorkoutActive.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime = _elapsedTime.asStateFlow()

    private val _showPauseDialog = MutableStateFlow(false)
    val showPauseDialog = _showPauseDialog.asStateFlow()

    private val _pauseCountdown = MutableStateFlow(30)
    val pauseCountdown = _pauseCountdown.asStateFlow()

    private var workoutStartTime = 0L
    private var profileData: ProfileResponse.Data? = null
    private var lastAccValues = Triple(0.0, 0.0, 0.0)
    private var stepThreshold = 15.0
    private var isPaused = false
    private var pauseStartTime = 0L

    // Thông số để tính bước chân từ gia tốc
    // Average stride length (mét) - có thể điều chỉnh dựa trên chiều cao người dùng
    private fun getStrideLength(): Double {
        val height = profileData?.profile?.height?.toDouble() ?: 170.0
        return 0.43 * (height / 100.0) // stride length tính theo chiều cao
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.send(Resource.Loading())
            try {
                when (val response = RemoteModule.mainApi.getProfile()) {
                    is Resource.Success -> {
                        profileData = response.data?.data
                        if (profileData != null) {
                            _profileState.send(Resource.Success(profileData!!))
                        }
                    }
                    is Resource.Error -> {
                        _profileState.send(Resource.Error(response.error))
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _profileState.send(Resource.Error(e as? CustomException ?: UnknownException(null, e.message, "getProfile")))
            }
        }
    }

    fun startWorkout(targetDistanceKm: Double) {
        _isWorkoutActive.value = true
        _showPauseDialog.value = false
        isPaused = false
        workoutStartTime = System.currentTimeMillis()
        _workoutStats.value = WorkoutStats(
            targetDistance = targetDistanceKm,
            isWorkoutCompleted = false
        )
        _elapsedTime.value = 0L

        viewModelScope.launch {
            while (isActive && _isWorkoutActive.value) {
                delay(1000)
                if (!isPaused) {
                    _elapsedTime.value = System.currentTimeMillis() - workoutStartTime
                }
                loadHeartRateForWorkout()
            }
        }
    }

    fun pauseWorkout() {
        isPaused = true
        _showPauseDialog.value = true
        pauseStartTime = System.currentTimeMillis()
        _pauseCountdown.value = 30

        // Countdown timer 30s
        viewModelScope.launch {
            for (i in 29 downTo 0) {
                delay(1000)
                if (_showPauseDialog.value) {
                    _pauseCountdown.value = i
                } else {
                    break
                }
            }

            // Nếu hết 30s mà không tiếp tục, reset lại từ đầu
            if (_showPauseDialog.value) {
                resetWorkout()
            }
        }
    }

    fun resumeWorkout() {
        isPaused = false
        _showPauseDialog.value = false
        // Adjust start time để tính elapsed time đúng khi pause
        workoutStartTime = System.currentTimeMillis() - _elapsedTime.value
    }

    fun stopWorkout() {
        _isWorkoutActive.value = false
        _showPauseDialog.value = false
        calculateWorkoutStats()
    }

    private fun loadHeartRateForWorkout() {
        viewModelScope.launch {
            try {
                val response = RemoteModule.mainApi.getRealtimeHeart()
                when (response) {
                    is Resource.Success -> {
                        val data = response.data?.data
                        if (data != null) {
                            val bpm = data.bpm ?: 0.0
                            if (bpm > 0) {
                                val currentStats = _workoutStats.value
                                val newReadings = currentStats.bpmReadings + bpm
                                _workoutStats.value = currentStats.copy(
                                    bpmReadings = newReadings
                                )
                            }

                            // Tính bước chân từ gia tốc
                            val accX = data.accX ?: 0.0
                            val accY = data.accY ?: 0.0
                            val accZ = data.accZ ?: 0.0
                            detectStep(accX, accY, accZ)
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                // Log error but continue workout
            }
        }
    }

    private fun detectStep(accX: Double, accY: Double, accZ: Double) {
        val currentTime = System.currentTimeMillis()

        // 1) Tính số bước chân (theo công thức chuẩn)
        val magnitude = sqrt(accX * accX + accY * accY + accZ * accZ)

        // 2) Lọc nhiễu nhẹ bằng EMA nếu bật
        val filteredMagnitude = if (USE_SMOOTHING) {
            // initialize if first time
            if (lastFilteredMagnitude == 0.0) {
                lastFilteredMagnitude = magnitude
            } else {
                lastFilteredMagnitude = SMOOTHING_ALPHA * magnitude + (1 - SMOOTHING_ALPHA) * lastFilteredMagnitude
            }
            lastFilteredMagnitude
        } else {
            magnitude
        }

        // 3) Phát hiện bước dựa trên ngưỡng + khoảng cách thời gian (thay vì so sánh lastMagnitude)
        if (filteredMagnitude > STEP_THRESHOLD && (currentTime - lastStepTimestamp > STEP_DELAY_MS)) {
            val currentStats = _workoutStats.value
            val newStepCount = currentStats.stepCount + 1

            // Mỗi bước ~0.4–0.5m → cộng dồn để mượt
            val strideLengthMeters = getStrideLength()       // vd: 0.43m
            val strideLengthKm = strideLengthMeters / 1000.0
            val newDistance = currentStats.currentDistance + strideLengthKm

            val updatedStats = currentStats.copy(
                stepCount = newStepCount,
                currentDistance = newDistance
            )
            _workoutStats.value = updatedStats

            lastStepTimestamp = currentTime

            // Check hoàn thành
            if (newDistance >= currentStats.targetDistance && currentStats.targetDistance > 0) {
                completeWorkout()
            }
        }


        // optional: vẫn lưu last raw acc nếu cần
        lastAccValues = Triple(accX, accY, accZ)
    }

    private fun completeWorkout() {
        _isWorkoutActive.value = false
        val stats = _workoutStats.value
        _workoutStats.value = stats.copy(isWorkoutCompleted = true)
        calculateWorkoutStats()
    }

    private fun calculateWorkoutStats() {
        val stats = _workoutStats.value
        val readings = stats.bpmReadings

        if (readings.isEmpty()) return

        val avgBpm = readings.average()
        val maxBpm = readings.maxOrNull() ?: 0.0
        val minBpm = readings.minOrNull() ?: 0.0
        val durationMinutes = _elapsedTime.value / 1000.0 / 60.0

        // Công thức tính calo: (avgBpm - 60) * 0.5 * weight * duration (phút) / 30
        val weight = profileData?.profile?.weight ?: 70
        val caloriesBurned = ((avgBpm - 60) * 0.5 * weight * durationMinutes) / 30

        _workoutStats.value = stats.copy(
            avgBpm = avgBpm,
            maxBpm = maxBpm,
            minBpm = minBpm,
            caloriesBurned = maxOf(0.0, caloriesBurned),
            duration = _elapsedTime.value
        )
    }

    fun resetWorkout() {
        _isWorkoutActive.value = false
        _showPauseDialog.value = false
        _workoutStats.value = WorkoutStats()
        _elapsedTime.value = 0L
        isPaused = false
        lastAccValues = Triple(0.0, 0.0, 0.0)
    }

    fun dismissPauseDialog() {
        _showPauseDialog.value = false
        isPaused = false
    }
}