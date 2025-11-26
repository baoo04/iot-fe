package com.ptit.iot.viewmodel

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptit.iot.DeviceUUID
import com.ptit.iot.NetworkResourceNotFoundException
import com.ptit.iot.RemoteModule
import com.ptit.iot.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MainViewModel : ViewModel() {

    // --- STATES (Trạng thái UI) ---
    private val _heartRateState = MutableStateFlow<HeartRateState>(HeartRateState.Loading)
    val heartRateState: StateFlow<HeartRateState> = _heartRateState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.NotConnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _scanState = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scanState: StateFlow<List<BluetoothDevice>> = _scanState.asStateFlow()

    private val _trainState = MutableStateFlow<TrainState>(TrainState.Idle)
    val trainState: StateFlow<TrainState> = _trainState.asStateFlow()

    private val _isWarningEnabled = MutableStateFlow(false)
    val isWarningEnabled: StateFlow<Boolean> = _isWarningEnabled.asStateFlow()

    private val _playWarningSound = Channel<Unit>()
    val playWarningSound = _playWarningSound.receiveAsFlow()

    // --- VARIABLES ---
    private var gatt: BluetoothGatt? = null
    private var heartRateJob: Job? = null
    private var userId: String? = null
    private val heartRateHistory = mutableListOf<Int>()
    private var lastAlertTimestamp: Long = 0
    private val ALERT_COOLDOWN = 5000L

    // --- STEP COUNTER VARIABLES (Đếm bước chân) ---
    private var stepCount = 0
    private var lastStepTimestamp: Long = 0
    // Ngưỡng phát hiện bước chân (m/s^2). Gia tốc trọng trường ~ 9.8.
    // Khi bước đi, xung lực sẽ vượt qua mức này.
    private val STEP_THRESHOLD = 10.5
    private val STEP_DELAY_MS = 300L // Khoảng cách tối thiểu giữa 2 bước (tránh đếm trùng)

    // --- FUNCTIONS ---

    fun setUserId(userId: String?) {
        this.userId = userId
    }

    // --- BLUETOOTH LOGIC ---
    fun addScannedDevice(device: BluetoothDevice) {
        val currentList = _scanState.value.toMutableList()
        if (!currentList.contains(device)) {
            currentList.add(device)
            _scanState.value = currentList
        }
    }

    fun clearScannedDevices() {
        _scanState.value = emptyList()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(device: BluetoothDevice?, context: Context) {
        // Chế độ test không cần thiết bị thật
        if (device == null) {
            _connectionState.value = ConnectionState.Connected
            startHeartRateUpdates()
            return
        }

        _connectionState.value = ConnectionState.Connecting

        gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _connectionState.value = ConnectionState.NotConnected
                    stopHeartRateUpdates()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(DeviceUUID.DEVICE_SERVICE_UUID)
                    if (service != null) {
                        Log.d("BLE", "Service discovered. Attempting to write UserID.")
                        userId?.let { uid -> writeUserIdToDevice(gatt, uid) }
                    } else {
                        _connectionState.value = ConnectionState.Error("Không tìm thấy Service UUID")
                        gatt.disconnect()
                    }
                } else {
                    _connectionState.value = ConnectionState.Error("Lỗi tìm Service: $status")
                    gatt.disconnect()
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _connectionState.value = ConnectionState.Connected
                    Log.d("BLE", "UserID written successfully. Starting API updates.")
                    startHeartRateUpdates()
                } else {
                    _connectionState.value = ConnectionState.Error("Lỗi gửi UserID: $status")
                    gatt.disconnect()
                }
            }
        })
    }

    private fun writeUserIdToDevice(gatt: BluetoothGatt, userId: String) {
        try {
            val service = gatt.getService(DeviceUUID.DEVICE_SERVICE_UUID)
            val characteristic = service.getCharacteristic(DeviceUUID.DEVICE_CHARACTERISTIC_UUID)

            if (characteristic == null) {
                _connectionState.value = ConnectionState.Error("Characteristic không tồn tại")
                gatt.disconnect()
                return
            }

            characteristic.value = userId.toByteArray(Charsets.UTF_8)
            gatt.writeCharacteristic(characteristic)
        } catch (e: Exception) {
            e.printStackTrace()
            _connectionState.value = ConnectionState.Error("Lỗi ghi UserID: ${e.message}")
            gatt.disconnect()
        }
    }

    fun disconnect() {
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (e: Exception) { e.printStackTrace() }
        gatt = null
        _connectionState.value = ConnectionState.NotConnected
        stopHeartRateUpdates()
        heartRateHistory.clear()
        stepCount = 0 // Reset bước chân khi ngắt kết nối
    }

    // --- API & DATA LOGIC ---
    private fun startHeartRateUpdates() {
        heartRateJob?.cancel()
        heartRateJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                fetchHeartRate()
                delay(1000) // Gọi API mỗi giây
            }
        }
    }

    private fun stopHeartRateUpdates() {
        heartRateJob?.cancel()
        heartRateJob = null
        _heartRateState.value = HeartRateState.Loading
    }

    private suspend fun fetchHeartRate() {
        try {
            val response = RemoteModule.mainApi.getRealtimeHeart()

            if (response is Resource.Success) {
                val data = response.data.data

                if (data != null) {
                    val currentBpm = data.bpm?.toInt() ?: 0
                    val spo2 = data.spo2?.toInt() ?: 0
                    val warningCode = data.warning ?: 0

                    val accX = data.accX ?: 0.0
                    val accY = data.accY ?: 0.0
                    val accZ = data.accZ ?: 0.0
                    val tempMpu = data.temp ?: 0.0

                    // --- LOGIC ĐẾM BƯỚC CHÂN (Step Counter) ---
                    // Tính độ lớn vector gia tốc (Magnitude)
                    val magnitude = sqrt(accX * accX + accY * accY + accZ * accZ)

                    // Kiểm tra ngưỡng (Threshold)
                    val currentTime = System.currentTimeMillis()
                    if (magnitude > STEP_THRESHOLD && (currentTime - lastStepTimestamp > STEP_DELAY_MS)) {
                        stepCount++
                        lastStepTimestamp = currentTime
                    }
                    // ------------------------------------------

                    // Cảnh báo âm thanh
                    if (warningCode > 0 && _isWarningEnabled.value) {
                        if (currentTime - lastAlertTimestamp >= ALERT_COOLDOWN) {
                            _playWarningSound.send(Unit)
                            lastAlertTimestamp = currentTime
                        }
                    }

                    // Logic thống kê BPM
                    if (currentBpm > 0) {
                        heartRateHistory.add(currentBpm)
                        if (heartRateHistory.size > 50) heartRateHistory.removeAt(0)

                        val minBpm = heartRateHistory.minOrNull() ?: currentBpm
                        val maxBpm = heartRateHistory.maxOrNull() ?: currentBpm
                        val avgBpm = if (heartRateHistory.isNotEmpty()) heartRateHistory.average().toInt() else currentBpm

                        _heartRateState.value = HeartRateState.Success(
                            currentBpm = currentBpm,
                            minBpm = minBpm,
                            maxBpm = maxBpm,
                            avgBpm = avgBpm,
                            warning = warningCode,
                            spo2 = spo2,
                            accX = accX,
                            accY = accY,
                            accZ = accZ,
                            tempMpu = tempMpu,
                            steps = stepCount // Truyền số bước vào State
                        )
                    } else {
                        // Nếu BPM = 0 nhưng có dữ liệu khác
                        if (accX != 0.0 || tempMpu != 0.0) {
                            _heartRateState.value = HeartRateState.Success(
                                currentBpm = 0, minBpm = 0, maxBpm = 0, avgBpm = 0, warning = 0,
                                spo2 = spo2, accX = accX, accY = accY, accZ = accZ, tempMpu = tempMpu,
                                steps = stepCount
                            )
                        } else {
                            _heartRateState.value = HeartRateState.NoData
                        }
                    }
                } else {
                    _heartRateState.value = HeartRateState.NoData
                }
            } else if (response is Resource.Error) {
                if (response.error is NetworkResourceNotFoundException) {
                    _heartRateState.value = HeartRateState.NoData
                } else {
                    _heartRateState.value = HeartRateState.Error(response.error.message ?: "Lỗi server")
                }
            }
        } catch (e: Exception) {
            _heartRateState.value = HeartRateState.Error(e.message ?: "Lỗi ngoại lệ")
        }
    }

    fun trainModel() {
        viewModelScope.launch(Dispatchers.IO) {
            _trainState.value = TrainState.Loading
            try {
                val response = RemoteModule.mainApi.trainModel()
                if (response is Resource.Success) {
                    _trainState.value = TrainState.Success
                } else {
                    val msg = if(response is Resource.Error) response.error.message else "Thất bại"
                    _trainState.value = TrainState.Error(msg ?: "Lỗi")
                }
            } catch (e: Exception) {
                _trainState.value = TrainState.Error(e.message ?: "Lỗi kết nối")
            }
        }
    }

    fun toggleWarning(isEnabled: Boolean) {
        _isWarningEnabled.value = isEnabled
        if (!isEnabled) lastAlertTimestamp = 0
    }
}

// --- DEFINE CÁC CLASS STATE ---

sealed class HeartRateState {
    data object Loading : HeartRateState()
    data object NoData : HeartRateState()
    data class Success(
        val currentBpm: Int,
        val minBpm: Int,
        val maxBpm: Int,
        val avgBpm: Int,
        val warning: Int,
        val spo2: Int,
        val accX: Double,
        val accY: Double,
        val accZ: Double,
        val tempMpu: Double,
        val steps: Int // Đã thêm trường Steps
    ) : HeartRateState()
    data class Error(val message: String) : HeartRateState()
}

sealed class ConnectionState {
    data object NotConnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

sealed class TrainState {
    data object Idle : TrainState()
    data object Loading : TrainState()
    data object Success : TrainState()
    data class Error(val message: String) : TrainState()
}