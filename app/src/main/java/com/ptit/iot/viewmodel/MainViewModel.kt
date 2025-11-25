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
import com.ptit.iot.HeartRateResponse
import com.ptit.iot.NetworkResourceNotFoundException
import com.ptit.iot.RemoteModule
import com.ptit.iot.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel : ViewModel() {
    private val _heartRateState = MutableStateFlow<HeartRateState>(HeartRateState.Loading)
    val heartRateState: StateFlow<HeartRateState> = _heartRateState

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.NotConnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _scanState = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scanState: StateFlow<List<BluetoothDevice>> = _scanState

    private var gatt: BluetoothGatt? = null
    private var heartRateJob: Job? = null
    private var userId: String? = null
    
    private val heartRateHistory = mutableListOf<Int>()

    fun setUserId(userId: String?) {
        this.userId = userId
    }

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

    @OptIn(ExperimentalStdlibApi::class)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(device: BluetoothDevice, context: Context) {
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
                        userId?.let { uid ->
                            writeUserIdToDevice(gatt, uid)
                        }
                    } else {
                        _connectionState.value = ConnectionState.Error("Heart rate service not found")
                        gatt.disconnect()
                    }
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                Log.d("taotest", "onCharacteristicWrite: ${characteristic.value.toString(Charsets.UTF_8)}")
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _connectionState.value = ConnectionState.Connected
                    startHeartRateUpdates()
                } else {
                    _connectionState.value = ConnectionState.Error("Failed to write user ID")
                    gatt.disconnect()
                }
            }
        })
    }

    private fun writeUserIdToDevice(gatt: BluetoothGatt, userId: String) {
        val service = gatt.getService(DeviceUUID.DEVICE_SERVICE_UUID)
        val characteristic = service.getCharacteristic(DeviceUUID.DEVICE_CHARACTERISTIC_UUID)

        characteristic.value = userId.toByteArray()

        gatt.writeCharacteristic(characteristic)
    }

    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _connectionState.value = ConnectionState.NotConnected
        stopHeartRateUpdates()
        heartRateHistory.clear()
    }

    private fun startHeartRateUpdates() {
        heartRateJob?.cancel()
        heartRateJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                fetchHeartRate()
                delay(1000) // Fetch every second
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
            when (response) {
                is Resource.Success -> {
                    val data = response.data.data
                    if (data != null) {
                        val currentBpm = data.bpm?.toInt() ?: 0
                        if (currentBpm > 0) {
                            heartRateHistory.add(currentBpm)
                            
                            val minBpm = heartRateHistory.minOrNull() ?: currentBpm
                            val maxBpm = heartRateHistory.maxOrNull() ?: currentBpm
                            val avgBpm = if (heartRateHistory.isNotEmpty()) {
                                heartRateHistory.average().toInt()
                            } else {
                                currentBpm
                            }
                            
                            _heartRateState.value = HeartRateState.Success(
                                currentBpm = currentBpm,
                                minBpm = minBpm,
                                maxBpm = maxBpm,
                                avgBpm = avgBpm,
                                warning = data.warning ?: 0
                            )
                        } else {
                            _heartRateState.value = HeartRateState.NoData
                        }
                    } else {
                        _heartRateState.value = HeartRateState.NoData
                    }
                }
                is Resource.Error -> {
                    // Treat 404 (NetworkResourceNotFoundException) as waiting for data
                    if (response.error is NetworkResourceNotFoundException) {
                        _heartRateState.value = HeartRateState.NoData
                    } else {
                        _heartRateState.value = HeartRateState.Error(response.error.message ?: "Unknown error")
                    }
                }
                else -> {
                    _heartRateState.value = HeartRateState.NoData
                }
            }
        } catch (e: Exception) {
            _heartRateState.value = HeartRateState.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class HeartRateState {
    data object Loading : HeartRateState()
    data object NoData : HeartRateState()
    data class Success(
        val currentBpm: Int,
        val minBpm: Int,
        val maxBpm: Int,
        val avgBpm: Int,
        val warning: Int
    ) : HeartRateState()
    data class Error(val message: String) : HeartRateState()
}

sealed class ConnectionState {
    data object NotConnected : ConnectionState()
    data object Connecting : ConnectionState() 
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
