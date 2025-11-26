package com.ptit.iot.ui.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.media.RingtoneManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptit.iot.BluetoothScanner
import com.ptit.iot.R
import com.ptit.iot.viewmodel.ConnectionState
import com.ptit.iot.viewmodel.HeartRateState
import com.ptit.iot.viewmodel.MainViewModel
import com.ptit.iot.viewmodel.TrainState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userId: String?,
    viewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val heartRateState by viewModel.heartRateState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val scannedDevices by viewModel.scanState.collectAsStateWithLifecycle()

    // NEW STATES: Lấy trạng thái Train và Cảnh báo từ ViewModel
    val trainState by viewModel.trainState.collectAsStateWithLifecycle()
    val isWarningEnabled by viewModel.isWarningEnabled.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    val bluetoothScanner = remember { BluetoothScanner(context) }

    // Set user ID when the screen is first composed
    LaunchedEffect(userId) {
        userId?.let { viewModel.setUserId(it) }
    }

    // NEW LOGIC: Lắng nghe sự kiện phát loa từ ViewModel
    LaunchedEffect(Unit) {
        viewModel.playWarningSound.collect {
            try {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(context, notification)
                r.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // NEW LOGIC: Hiển thị Toast khi Train xong hoặc Lỗi
    LaunchedEffect(trainState) {
        when (trainState) {
            is TrainState.Success -> {
                Toast.makeText(context, "Huấn luyện AI thành công!", Toast.LENGTH_SHORT).show()
            }
            is TrainState.Error -> {
                val msg = (trainState as TrainState.Error).message
                Toast.makeText(context, "Lỗi: $msg", Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top heart icon and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.heart_small),
                    contentDescription = "Heart icon",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nhịp tim & AI", // Cập nhật tiêu đề nhẹ
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Different UI based on connection state
            when (connectionState) {
                is ConnectionState.NotConnected -> {
                    EmptyStateContent(
                        onScanClick = {
//                            if (bluetoothScanner.hasBluetoothPermissions() && bluetoothScanner.isBluetoothEnabled()) {
                            viewModel.clearScannedDevices()
                            showBottomSheet = true
                            scope.launch {
                                bluetoothScanner.startScan { device ->
                                    viewModel.addScannedDevice(device)
                                }
                            }
//                            }
                        }
                    )
                }

                is ConnectionState.Connecting -> {
                    ConnectionLoadingContent()
                }

                is ConnectionState.Connected -> {
                    when (heartRateState) {
                        is HeartRateState.Success -> {
                            val data = heartRateState as HeartRateState.Success
                            HeartRateContent(
                                currentBpm = data.currentBpm,
                                minBpm = data.minBpm,
                                maxBpm = data.maxBpm,
                                avgBpm = data.avgBpm,
                                warning = data.warning,
                                // Truyền các biến mới xuống
                                isWarningEnabled = isWarningEnabled,
                                trainState = trainState,
                                onToggleWarning = { viewModel.toggleWarning(it) },
                                onTrainModel = { viewModel.trainModel() },
                                onDisconnect = { viewModel.disconnect() }
                            )
                        }

                        is HeartRateState.NoData, is HeartRateState.Loading -> {
                            WaitingForDataContent(
                                onDisconnect = { viewModel.disconnect() }
                            )
                        }

                        is HeartRateState.Error -> {
                            val error = heartRateState as HeartRateState.Error
                            ErrorContent(
                                message = error.message,
                                onRetry = { /* Retry logic */ },
                                onDisconnect = { viewModel.disconnect() }
                            )
                        }
                    }
                }

                is ConnectionState.Error -> {
                    val error = connectionState as ConnectionState.Error
                    ErrorContent(
                        message = error.message,
                        onRetry = { /* Retry connection */ },
                        onDisconnect = { viewModel.disconnect() }
                    )
                }
            }
        }
    }

    // Bottom sheet for device scanning
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                bluetoothScanner.stopScan()
            },
            sheetState = sheetState
        ) {
            DeviceScannerContent(
                devices = scannedDevices,
                onDeviceSelected = { device ->
                    viewModel.connectToDevice(device, context)
                    showBottomSheet = false
                    bluetoothScanner.stopScan()
                },
                onScanAgain = {
                    viewModel.clearScannedDevices()
                    bluetoothScanner.startScan { device ->
                        viewModel.addScannedDevice(device)
                    }
                }
            )
        }
    }
}

@Composable
fun EmptyStateContent(onScanClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.main_heart),
            contentDescription = "Heart Icon",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Không có thiết bị được kết nối",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Vui lòng kết nối với thiết bị IOT để bắt đầu theo dõi nhịp tim",
            style = TextStyle(
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onScanClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFDF1F32)
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Quét và Kết nối thiết bị",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
fun ConnectionLoadingContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = Color(0xFFDF1F32),
            modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Đang kết nối...",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun WaitingForDataContent(onDisconnect: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Heart rate circle with gradient border (loading state)
        Box(
            modifier = Modifier
                .size(206.dp)
                .clip(CircleShape)
                .border(
                    width = 4.dp,
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF6B6B),
                            Color(0xFFDF1F32),
                            Color(0xFFFF6B6B)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Heart icon as background
            Image(
                painter = painterResource(id = R.drawable.main_heart),
                contentDescription = "Heart",
                modifier = Modifier.size(140.dp)
            )

            // Loading indicator overlay
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = (-5).dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFF5F5F5),
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Đang đợi",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFFF5F5F5),
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Info boxes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoBox(
                value = "0",
                label = "Min",
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            InfoBox(
                value = "0",
                label = "Trung bình",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )

            InfoBox(
                value = "0",
                label = "Max",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xCCC6F6AB))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Đang chờ dữ liệu",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Đang chờ nhận dữ liệu từ thiết bị...",
                    style = TextStyle(
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onDisconnect,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFDF1F32)
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFDF1F32))
            )
        ) {
            Text("Ngắt kết nối")
        }
    }
}

@Composable
fun HeartRateContent(
    currentBpm: Int,
    minBpm: Int,
    maxBpm: Int,
    avgBpm: Int,
    warning: Int,
    // NEW PARAMS cho chức năng AI
    isWarningEnabled: Boolean,
    trainState: TrainState,
    onToggleWarning: (Boolean) -> Unit,
    onTrainModel: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Heart rate circle with gradient border
        Box(
            modifier = Modifier
                .size(206.dp)
                .clip(CircleShape)
                .border(
                    width = 4.dp,
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFDF1F32),
                            Color(0xFFFF6B6B),
                            Color(0xFFFFC107)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Heart icon as background
            Image(
                painter = painterResource(id = R.drawable.main_heart),
                contentDescription = "Heart",
                modifier = Modifier.size(140.dp)
            )

            // BPM text overlay on heart
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = (-5).dp)
            ) {
                Text(
                    text = "$currentBpm",
                    style = TextStyle(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF5F5F5)
                    )
                )
                Text(
                    text = "bpm",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFFF5F5F5)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info boxes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoBox(
                value = minBpm.toString(),
                label = "Min",
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            InfoBox(
                value = avgBpm.toString(),
                label = "Trung bình",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )

            InfoBox(
                value = maxBpm.toString(),
                label = "Max",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status box with different background based on warning level
        val (statusColor, statusTitle, statusDescription) = when (warning) {
            0 -> Triple(
                Color(0xCCC6F6AB),
                "Bình thường",
                "Nhịp tim ổn định, không phát hiện bất thường !!!"
            )

            1 -> Triple(
                Color(0xCCF6E3AB),
                "Cảnh báo",
                "Nhịp tim hơi cao, hãy thư giãn một chút."
            )

            else -> Triple(
                Color(0xCCF6ABAB),
                "Nguy hiểm",
                "Nhịp tim bất thường, hãy liên hệ bác sĩ ngay!!!"
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(statusColor)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = statusTitle,
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = statusDescription,
                    style = TextStyle(
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =====================================================================
        // NEW FEATURE: BẢNG ĐIỀU KHIỂN AI & CẢNH BÁO
        // =====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Cài đặt AI & Cảnh báo",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Switch Bật/Tắt Loa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Sound",
                            tint = if (isWarningEnabled) Color(0xFFDF1F32) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Phát loa cảnh báo",
                                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = if (isWarningEnabled) "Đang bật" else "Đang tắt",
                                style = TextStyle(fontSize = 12.sp, color = Color.Gray)
                            )
                        }
                    }
                    Switch(
                        checked = isWarningEnabled,
                        onCheckedChange = onToggleWarning,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFDF1F32),
                            checkedTrackColor = Color(0xFFFFCDD2)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // 2. Button Train Model
                val isTraining = trainState is TrainState.Loading

                Button(
                    onClick = onTrainModel,
                    enabled = !isTraining,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDF1F32),
                        disabledContainerColor = Color(0xFFFFCDD2)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isTraining) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang huấn luyện...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Huấn luyện lại mô hình AI")
                    }
                }
            }
        }
        // =====================================================================

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onDisconnect,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFDF1F32)
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFDF1F32))
            )
        ) {
            Text("Ngắt kết nối")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun InfoBox(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(104.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE9F5F9))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = TextStyle(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                )
            )

            Text(
                text = label,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}

@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.main_heart),
            contentDescription = "Heart Icon",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Lỗi kết nối",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = TextStyle(
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onDisconnect,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFDF1F32)
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFDF1F32))
                )
            ) {
                Text("Ngắt kết nối")
            }

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDF1F32)
                )
            ) {
                Text("Thử lại")
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceScannerContent(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onScanAgain: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Quét thiết bị Bluetooth",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFDF1F32))
            }

            Text(
                text = "Đang quét thiết bị...",
                style = TextStyle(
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            devices.forEach { device ->
                DeviceItem(
                    deviceName = device.name ?: "Unknown Device",
                    deviceAddress = device.address,
                    onClick = { onDeviceSelected(device) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onScanAgain,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDF1F32)
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Quét lại")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DeviceItem(
    deviceName: String,
    deviceAddress: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = deviceName,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = deviceAddress,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
        }
    }
}