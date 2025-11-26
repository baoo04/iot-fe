package com.ptit.iot.ui.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptit.iot.BluetoothScanner
import com.ptit.iot.R
import com.ptit.iot.viewmodel.ConnectionState
import com.ptit.iot.viewmodel.HeartRateState
import com.ptit.iot.viewmodel.MainViewModel
import com.ptit.iot.viewmodel.TrainState
import kotlinx.coroutines.launch

// Màu cam đậm đỏ chuyên nghiệp (darker orange-red theme)
val PrimaryDarkOrange = Color(0xFFD32F2F) // Màu cam đỏ đậm chính
val SecondaryDarkOrange = Color(0xFFE57373) // Màu cam đỏ nhạt
val AccentDarkOrange = Color(0xFFEF5350) // Màu nhấn cam đỏ
val BackgroundDark = Color(0xFFFAFAFA) // Nền nhạt nhưng chuyên nghiệp
val WarningYellow = Color(0xFFFFEB3B)
val DangerRed = Color(0xFFB71C1C) // Đỏ đậm hơn
val SuccessGreen = Color(0xFF388E3C)
val TextDark = Color(0xFF212121)
val TextLight = Color(0xFF757575)

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
    val trainState by viewModel.trainState.collectAsStateWithLifecycle()
    val isWarningEnabled by viewModel.isWarningEnabled.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    val bluetoothScanner = remember { BluetoothScanner(context) }
    val mediaPlayer = remember { MediaPlayer.create(context, R.drawable.heart_small) } // Sửa thành raw sound

    // Xử lý phát âm thanh cảnh báo
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.playWarningSound.collect {
                if (isWarningEnabled) {
                    mediaPlayer.start()
                }
            }
        }
    }

    LaunchedEffect(userId) {
        userId?.let { viewModel.setUserId(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theo Dõi Sức Khỏe Thông Minh", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryDarkOrange,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (connectionState) {
                    is ConnectionState.NotConnected -> {
                        EmptyStateContent(
                            onScanClick = {
                                viewModel.clearScannedDevices()
                                showBottomSheet = true
                                scope.launch {
                                    bluetoothScanner.startScan { device ->
                                        viewModel.addScannedDevice(device)
                                    }
                                }
                            }
                        )
                    }

                    is ConnectionState.Connecting -> {
                        ConnectionLoadingContent()
                    }

                    is ConnectionState.Connected -> {
                        ConnectedContent(
                            heartRateState = heartRateState,
                            trainState = trainState,
                            isWarningEnabled = isWarningEnabled,
                            onDisconnect = { viewModel.disconnect() },
                            onTrainModel = { viewModel.trainModel() },
                            onToggleWarning = { viewModel.toggleWarning(it) }
                        )
                    }

                    is ConnectionState.Error -> {
                        val error = connectionState as ConnectionState.Error
                        ErrorContent(
                            message = error.message,
                            onRetry = { viewModel.disconnect() },
                            onDisconnect = { viewModel.disconnect() }
                        )
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    bluetoothScanner.stopScan()
                },
                sheetState = sheetState,
                containerColor = BackgroundDark
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
}

@Composable
fun ConnectedContent(
    heartRateState: HeartRateState,
    trainState: TrainState,
    isWarningEnabled: Boolean,
    onDisconnect: () -> Unit,
    onTrainModel: () -> Unit,
    onToggleWarning: (Boolean) -> Unit
) {
    when (heartRateState) {
        is HeartRateState.Success -> {
            val data = heartRateState
            HeartRateContent(
                currentBpm = data.currentBpm,
                minBpm = data.minBpm,
                maxBpm = data.maxBpm,
                avgBpm = data.avgBpm,
                warning = data.warning,
                spo2 = data.spo2,
                accX = data.accX,
                accY = data.accY,
                accZ = data.accZ,
                tempMpu = data.tempMpu,
                steps = data.steps,
                isWarningEnabled = isWarningEnabled,
                onToggleWarning = onToggleWarning,
                onDisconnect = onDisconnect
            )
        }

        is HeartRateState.NoData, is HeartRateState.Loading -> {
            WaitingForDataContent(
                onDisconnect = onDisconnect
            )
        }

        is HeartRateState.Error -> {
            val error = heartRateState as HeartRateState.Error
            ErrorContent(
                message = error.message,
                onRetry = onDisconnect,
                onDisconnect = onDisconnect
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Phần Train Model
    TrainModelSection(
        trainState = trainState,
        onTrainClick = onTrainModel
    )
}

@Composable
fun HeartRateContent(
    currentBpm: Int,
    minBpm: Int,
    maxBpm: Int,
    avgBpm: Int,
    warning: Int,
    spo2: Int,
    accX: Double,
    accY: Double,
    accZ: Double,
    tempMpu: Double,
    steps: Int,
    isWarningEnabled: Boolean,
    onToggleWarning: (Boolean) -> Unit,
    onDisconnect: () -> Unit,
) {
    val pulseScale by animateFloatAsState(
        targetValue = if (currentBpm > 0) 1.1f else 1f,
        animationSpec = tween(durationMillis = 500)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Phần Nhịp Tim với animation
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .shadow(12.dp, CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentDarkOrange, PrimaryDarkOrange)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.main_heart),
                contentDescription = "Heart",
                modifier = Modifier
                    .size(150.dp)
                    .scale(pulseScale)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = (-10).dp)
            ) {
                Text(
                    text = "$currentBpm",
                    style = TextStyle(
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "bpm",
                    style = TextStyle(fontSize = 18.sp, color = Color.White.copy(alpha = 0.8f))
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoBox(value = minBpm.toString(), label = "Min", modifier = Modifier.weight(1f).padding(4.dp))
            InfoBox(value = avgBpm.toString(), label = "TB", modifier = Modifier.weight(1f).padding(4.dp))
            InfoBox(value = maxBpm.toString(), label = "Max", modifier = Modifier.weight(1f).padding(4.dp))
        }

        // Phần SPO2
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SecondaryDarkOrange),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SpO2",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                )
                Text(
                    text = "$spo2%",
                    style = TextStyle(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (spo2 >= 95) SuccessGreen else DangerRed
                    )
                )
            }
        }

        // Phần Số Bước Chân
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Số Bước Chân",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextLight
                        )
                    )
                    Text(
                        text = "$steps",
                        style = TextStyle(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkOrange
                        )
                    )
                    Text(
                        text = "bước",
                        style = TextStyle(fontSize = 14.sp, color = TextLight)
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.main_heart), // Giả sử icon footsteps đúng
                    contentDescription = "Steps Icon",
                    tint = PrimaryDarkOrange,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Phần MPU6050 & Nhiệt Độ
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Cảm Biến Chuyển Động & Nhiệt Độ",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryDarkOrange
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nhiệt Độ Chip:",
                        style = TextStyle(fontSize = 16.sp, color = TextLight)
                    )
                    Text(
                        text = String.format("%.1f °C", tempMpu),
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentDarkOrange
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Gia Tốc (m/s²):",
                    style = TextStyle(fontSize = 16.sp, color = TextLight)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AxisBox(label = "X", value = accX, color = DangerRed)
                    AxisBox(label = "Y", value = accY, color = SuccessGreen)
                    AxisBox(label = "Z", value = accZ, color = PrimaryDarkOrange)
                }
            }
        }

        // Phần Trạng Thái Cảnh Báo - Làm đẹp hơn với animation rung icon
        Spacer(modifier = Modifier.height(32.dp))
        val (statusColor, statusTitle, statusDescription) = when (warning) {
            0 -> Triple(SuccessGreen.copy(alpha = 0.2f), "Bình Thường", "Nhịp tim ổn định, sức khỏe tốt.")
            1 -> Triple(WarningYellow.copy(alpha = 0.2f), "Cảnh Báo", "Nhịp tim hơi cao, hãy nghỉ ngơi.")
            else -> Triple(DangerRed.copy(alpha = 0.2f), "Nguy Hiểm", "Nhịp tim bất thường, liên hệ bác sĩ ngay!")
        }

        val infiniteTransition = rememberInfiniteTransition()
        val rotation by infiniteTransition.animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(200),
                repeatMode = RepeatMode.Reverse
            )
        )

        AnimatedVisibility(visible = warning > 0, enter = fadeIn(), exit = fadeOut()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .background(statusColor.copy(alpha = 0.8f)),
                colors = CardDefaults.cardColors(containerColor = statusColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = if (warning == 1) WarningYellow else DangerRed,
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(if (warning > 1) rotation else 0f) // Rung icon nếu nguy hiểm
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = statusTitle,
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        )
                        Text(
                            text = statusDescription,
                            style = TextStyle(fontSize = 14.sp, color = TextDark, textAlign = TextAlign.Start)
                        )
                    }
                }
            }
        }

        // Toggle Cảnh Báo Âm Thanh
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Bật Cảnh Báo Âm Thanh (Loa)",
                style = TextStyle(fontSize = 16.sp, color = TextDark)
            )
            Switch(
                checked = isWarningEnabled,
                onCheckedChange = onToggleWarning,
                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryDarkOrange)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDisconnect,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkOrange),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
        ) {
            Text("Ngắt Kết Nối", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun TrainModelSection(
    trainState: TrainState,
    onTrainClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Huấn Luyện Mô Hình AI",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryDarkOrange
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Huấn luyện lại mô hình để cải thiện độ chính xác dự đoán.",
                style = TextStyle(fontSize = 14.sp, color = TextLight)
            )
            Spacer(modifier = Modifier.height(16.dp))
            when (trainState) {
                is TrainState.Loading -> {
                    CircularProgressIndicator(color = PrimaryDarkOrange, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is TrainState.Success -> {
                    Text("Huấn luyện thành công!", color = SuccessGreen, fontWeight = FontWeight.Bold)
                }
                is TrainState.Error -> {
                    Text("Lỗi: ${trainState.message}", color = DangerRed)
                }
                else -> {}
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onTrainClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentDarkOrange),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Bắt Đầu Huấn Luyện", color = Color.White)
            }
        }
    }
}

@Composable
fun AxisBox(label: String, value: Double, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        )
        Text(
            text = String.format("%.2f", value),
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        )
    }
}

@Composable
fun WaitingForDataContent(onDisconnect: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.main_heart),
                contentDescription = null,
                modifier = Modifier.size(150.dp)
            )
            CircularProgressIndicator(
                color = PrimaryDarkOrange,
                modifier = Modifier.size(220.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            InfoBox(value = "--", label = "Min", modifier = Modifier.weight(1f).padding(4.dp))
            InfoBox(value = "--", label = "TB", modifier = Modifier.weight(1f).padding(4.dp))
            InfoBox(value = "--", label = "Max", modifier = Modifier.weight(1f).padding(4.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Đang đồng bộ dữ liệu từ thiết bị... Vui lòng chờ giây lát.",
            style = TextStyle(color = TextLight, fontSize = 16.sp, textAlign = TextAlign.Center)
        )

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onDisconnect,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkOrange)
        ) { Text("Hủy Kết Nối", color = Color.White) }
    }
}

@Composable
fun InfoBox(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueFontSize: androidx.compose.ui.unit.TextUnit = 30.sp
) {
    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = TextStyle(fontSize = valueFontSize, fontWeight = FontWeight.Bold, color = PrimaryDarkOrange)
            )
            Text(text = label, style = TextStyle(fontSize = 16.sp, color = TextLight))
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
        Text("Chưa Kết Nối Thiết Bị", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Nhấn nút bên dưới để quét và kết nối.", color = TextLight, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onScanClick,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkOrange)
        ) {
            Text("Quét Thiết Bị Bluetooth", color = Color.White)
        }
    }
}

@Composable
fun ConnectionLoadingContent() {
    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryDarkOrange, strokeWidth = 4.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Đang Kết Nối Bluetooth... Vui lòng chờ.", color = TextDark)
        }
    }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit, onDisconnect: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = DangerRed,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Lỗi: $message", color = DangerRed, textAlign = TextAlign.Center, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            OutlinedButton(
                onClick = onDisconnect,
                border = BorderStroke(1.dp, PrimaryDarkOrange)
            ) { Text("Ngắt Kết Nối", color = PrimaryDarkOrange) }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkOrange)
            ) { Text("Thử Lại", color = Color.White) }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceScannerContent(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onScanAgain: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Chọn Thiết Bị Bluetooth", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(16.dp))
        if (devices.isEmpty()) {
            Text("Đang quét thiết bị... Vui lòng chờ.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = TextLight)
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = PrimaryDarkOrange)
        } else {
            devices.forEach { device ->
                DeviceItem(deviceName = device.name ?: "Thiết Bị Không Tên", deviceAddress = device.address, onClick = { onDeviceSelected(device) })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onScanAgain,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkOrange),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text("Quét Lại", color = Color.White) }
    }
}

@Composable
fun DeviceItem(deviceName: String, deviceAddress: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp)),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.main_heart), // Giả sử icon bluetooth đúng
                contentDescription = "Bluetooth",
                tint = PrimaryDarkOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(deviceName, fontWeight = FontWeight.Bold, color = TextDark)
                Text(deviceAddress, fontSize = 12.sp, color = TextLight)
            }
        }
    }
}