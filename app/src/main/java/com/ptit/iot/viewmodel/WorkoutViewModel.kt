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

data class WorkoutStats(
    val duration: Long = 0, // ms
    val bpmReadings: List<Double> = emptyList(),
    val avgBpm: Double = 0.0,
    val maxBpm: Double = 0.0,
    val minBpm: Double = 0.0,
    val caloriesBurned: Double = 0.0
)

class WorkoutViewModel : ViewModel() {
    private val _heartState = Channel<Resource<HeartRateResponse>>()
    val heartState = _heartState.receiveAsFlow()

    private val _profileState = Channel<Resource<ProfileResponse.Data>>()
    val profileState = _profileState.receiveAsFlow()

    private val _workoutStats = MutableStateFlow<WorkoutStats>(WorkoutStats())
    val workoutStats = _workoutStats.asStateFlow()

    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive = _isWorkoutActive.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime = _elapsedTime.asStateFlow()

    private var workoutStartTime = 0L
    private var profileData: ProfileResponse.Data? = null

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

    fun startWorkout() {
        _isWorkoutActive.value = true
        workoutStartTime = System.currentTimeMillis()
        _workoutStats.value = WorkoutStats()
        _elapsedTime.value = 0L

        viewModelScope.launch {
            while (isActive && _isWorkoutActive.value) {
                delay(1000)
                _elapsedTime.value = System.currentTimeMillis() - workoutStartTime
                loadHeartRateForWorkout()
            }
        }
    }

    fun stopWorkout() {
        _isWorkoutActive.value = false
        calculateWorkoutStats()
    }

    private fun loadHeartRateForWorkout() {
        viewModelScope.launch {
            try {
                val response = RemoteModule.mainApi.getRealtimeHeart()
                when (response) {
                    is Resource.Success -> {
                        val bpm = response.data?.data?.bpm ?: 0.0
                        if (bpm > 0) {
                            val currentStats = _workoutStats.value
                            val newReadings = currentStats.bpmReadings + bpm
                            _workoutStats.value = currentStats.copy(
                                bpmReadings = newReadings
                            )
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                // Log error but continue workout
            }
        }
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
        _workoutStats.value = WorkoutStats()
        _elapsedTime.value = 0L
    }
}