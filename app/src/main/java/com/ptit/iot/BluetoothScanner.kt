package com.ptit.iot

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@SuppressLint("MissingPermission")
class BluetoothScanner(private val context: Context) {
    private val bluetoothManager: BluetoothManager? = ContextCompat.getSystemService(
        context,
        BluetoothManager::class.java
    )
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner = bluetoothAdapter?.bluetoothLeScanner
    private val handler = Handler(Looper.getMainLooper())

    private var scanning = false
    private var scanCallback: ScanCallback? = null

    fun isBluetoothEnabled() = bluetoothAdapter?.isEnabled == true

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        } else
            true
    }

    fun startScan(onDeviceFound: (BluetoothDevice) -> Unit) {
        if (!hasBluetoothPermissions() || !isBluetoothEnabled() || scanning) {
            return
        }

        val scanPeriod = 60000L // 60 seconds

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                result.device?.let { device ->
                    onDeviceFound(device)
                }
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                super.onBatchScanResults(results)
                for (result in results) {
                    result.device?.let { device ->
                        onDeviceFound(device)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.e("taotest", "Scan failed with error code: $errorCode")
            }
        }

        scanning = true

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        val heartRateServiceUuid = ParcelUuid(DeviceUUID.DEVICE_SERVICE_UUID)
        val filters = listOf(ScanFilter.Builder().setServiceUuid(heartRateServiceUuid).build())
        scanner?.startScan(filters, settings, scanCallback)

        handler.postDelayed({
            stopScan()
        }, scanPeriod)
    }

    fun stopScan() {
        if (!hasBluetoothPermissions() || !isBluetoothEnabled() || !scanning) {
            return
        }

        scanning = false
        scanCallback?.let {
            scanner?.stopScan(it)
        }
    }
}