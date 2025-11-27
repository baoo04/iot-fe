package com.ptit.iot.ui.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

// --- BẢNG MÀU HIỆN ĐẠI (Theme Y tế/Sức khỏe) ---
val ColorPrimary = Color(0xFFE91E63) // Hồng đậm chủ đạo
val ColorBackground = Color(0xFFF5F7FA) // Xám xanh nhạt (nền)
val ColorSurface = Color(0xFFFFFFFF) // Trắng (Card)
val ColorSuccess = Color(0xFF4CAF50) // Xanh lá (Bình thường)
val ColorWarning = Color(0xFFFFC107) // Vàng (Cảnh báo)
val ColorDanger = Color(0xFFD32F2F) // Đỏ (Nguy hiểm)
val ColorTextPrimary = Color(0xFF2D3436) // Đen xám
val ColorTextSecondary = Color(0xFF636E72) // Xám ghi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userId: String?,
    viewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current

    // Lấy trạng thái từ ViewModel
    val heartRateState by viewModel.heartRateState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val scannedDevices by viewModel.scanState.collectAsStateWithLifecycle()
    val trainState by viewModel.trainState.collectAsStateWithLifecycle()

    // Quản lý BottomSheet (Danh sách thiết bị)
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    // Scanner Bluetooth
    val bluetoothScanner = remember { BluetoothScanner(context) }

    // Cập nhật UserID khi vào màn hình
    LaunchedEffect(userId) {
        userId?.let { viewModel.setUserId(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Theo Dõi Sức Khỏe",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ColorSurface,
                    titleContentColor = ColorPrimary
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        containerColor = ColorBackground
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Nội dung chính cuộn được
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
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
                            onDisconnect = { viewModel.disconnect() },
                            onTrainModel = { viewModel.trainModel() }
                        )
                    }
                    is ConnectionState.Error -> {
                        ErrorContent(
                            message = (connectionState as ConnectionState.Error).message,
                            onRetry = { viewModel.disconnect() }
                        )
                    }
                }
            }
        }

        // BottomSheet hiển thị danh sách thiết bị quét được
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    bluetoothScanner.stopScan()
                },
                sheetState = sheetState,
                containerColor = ColorSurface
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
                        bluetoothScanner.startScan { device -> viewModel.addScannedDevice(device) }
                    }
                )
            }
        }
    }
}

// --- GIAO DIỆN KHI ĐÃ KẾT NỐI ---
@Composable
fun ConnectedContent(
    heartRateState: HeartRateState,
    trainState: TrainState,
    onDisconnect: () -> Unit,
    onTrainModel: () -> Unit
) {
    when (heartRateState) {
        is HeartRateState.Success -> {
            val data = heartRateState

            // 1. Banner Trạng Thái Sức Khỏe (Thay cho âm thanh)
            HealthStatusBanner(warningCode = data.warning)

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Khu vực Nhịp tim (Trái tim lớn)
            HeartRateSection(
                bpm = data.currentBpm,
                min = data.minBpm,
                max = data.maxBpm,
                avg = data.avgBpm
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Grid thông tin (SpO2 và Bước chân)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thẻ SpO2
                StatCard(
                    title = "SpO2",
                    value = "${data.spo2}%",
                    iconRes = R.drawable.main_heart, // Nếu không có icon phổi, dùng tạm tim
                    color = if(data.spo2 >= 95) ColorSuccess else ColorWarning,
                    modifier = Modifier.weight(1f)
                )
                // Thẻ Bước chân
                StatCard(
                    title = "Bước chân",
                    value = "${data.steps}",
                    iconRes = R.drawable.main_heart, // Icon bước chân
                    color = Color.Blue,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Thẻ Môi trường & Chuyển động
            EnvironmentCard(
                temp = data.tempMpu,
                accX = data.accX,
                accY = data.accY,
                accZ = data.accZ
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Nút Huấn luyện Model
            TrainModelSection(trainState, onTrainModel)

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Nút Ngắt kết nối
            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorDanger),
                border = BorderStroke(1.dp, ColorDanger)
            ) {
                Text("Ngắt kết nối thiết bị", fontWeight = FontWeight.SemiBold)
            }
        }

        is HeartRateState.NoData, is HeartRateState.Loading -> {
            // Màn hình chờ dữ liệu
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 50.dp)) {
                CircularProgressIndicator(color = ColorPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Đang đồng bộ dữ liệu...", color = ColorTextSecondary)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onDisconnect, colors = ButtonDefaults.buttonColors(containerColor = ColorTextSecondary)) {
                    Text("Hủy bỏ")
                }
            }
        }

        is HeartRateState.Error -> {
            ErrorContent(message = (heartRateState as HeartRateState.Error).message, onRetry = onDisconnect)
        }
    }
}

// --- CÁC COMPONENT CON (WIDGETS) ---

@Composable
fun HealthStatusBanner(warningCode: Int) {
    // Logic chọn màu và nội dung dựa trên warningCode
    val (bgColor, textColor, text, subText) = when (warningCode) {
        0 -> Quadruple(ColorSuccess, Color.White, "BÌNH THƯỜNG", "Nhịp tim ổn định, sức khỏe tốt.")
        1 -> Quadruple(ColorWarning, ColorTextPrimary, "CẢNH BÁO", "Nhịp tim hơi cao, hãy nghỉ ngơi.")
        else -> Quadruple(ColorDanger, Color.White, "NGUY HIỂM", "Nhịp tim bất thường! Cần kiểm tra ngay.")
    }

    // Animation nhấp nháy nếu là báo động đỏ
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (warningCode > 1) 0.6f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }, // Áp dụng animation
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor))
                Text(subText, style = TextStyle(fontSize = 14.sp, color = textColor.copy(alpha = 0.9f)))
            }
        }
    }
}

@Composable
fun HeartRateSection(bpm: Int, min: Int, max: Int, avg: Int) {
    // Animation tim đập
    val pulseScale by animateFloatAsState(
        targetValue = if (bpm > 0) 1.15f else 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Vòng tròn nền mờ
                Box(modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(ColorPrimary.copy(alpha = 0.1f)))

                // Icon trái tim (Bạn có thể thay bằng Image nếu muốn dùng ảnh png)
                Icon(
                    imageVector = Icons.Rounded.MonitorHeart,
                    contentDescription = null,
                    tint = ColorPrimary,
                    modifier = Modifier.size(80.dp).scale(pulseScale)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$bpm",
                style = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, color = ColorPrimary)
            )
            Text("Nhịp tim (BPM)", color = ColorTextSecondary, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = ColorBackground, thickness = 2.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Hàng thống kê Min/Avg/Max
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MiniStatItem("Thấp nhất", "$min")
                MiniStatItem("Trung bình", "$avg")
                MiniStatItem("Cao nhất", "$max")
            }
        }
    }
}

@Composable
fun MiniStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ColorTextPrimary)
        Text(label, fontSize = 12.sp, color = ColorTextSecondary)
    }
}

@Composable
fun StatCard(title: String, value: String, iconRes: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Dùng icon vector có sẵn hoặc painterResource
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = ColorTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
        }
    }
}

@Composable
fun EnvironmentCard(temp: Double, accX: Double, accY: Double, accZ: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Cảm biến MPU6050", fontWeight = FontWeight.Bold, color = ColorTextPrimary, fontSize = 16.sp)
            Divider(modifier = Modifier.padding(vertical = 12.dp), color = ColorBackground, thickness = 2.dp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Nhiệt độ", fontSize = 12.sp, color = ColorTextSecondary)
                    Text("${String.format("%.1f", temp)}°C", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = ColorPrimary)
                }

                // Tính độ lớn gia tốc
                val totalAcc = kotlin.math.sqrt(accX*accX + accY*accY + accZ*accZ)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Gia tốc tổng", fontSize = 12.sp, color = ColorTextSecondary)
                    Text("${String.format("%.2f", totalAcc)} m/s²", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = ColorTextPrimary)
                }
            }
        }
    }
}

@Composable
fun TrainModelSection(state: TrainState, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Cập nhật mô hình AI", fontWeight = FontWeight.Bold, color = ColorTextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Huấn luyện lại để tăng độ chính xác", fontSize = 12.sp, color = ColorTextSecondary)

            Spacer(modifier = Modifier.height(16.dp))

            if (state is TrainState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ColorPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Đang xử lý...", fontSize = 12.sp, color = ColorPrimary)
            } else {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Huấn Luyện Ngay")
                }
            }

            if (state is TrainState.Success) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Thành công!", color = ColorSuccess, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            if (state is TrainState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.message, color = ColorDanger, fontSize = 14.sp)
            }
        }
    }
}

// --- MÀN HÌNH CHƯA KẾT NỐI & SCAN ---

@Composable
fun EmptyStateContent(onScanClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 60.dp)
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Bluetooth,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.LightGray
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Chưa kết nối thiết bị", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = ColorTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Vui lòng kết nối với thiết bị ESP32\nđể bắt đầu theo dõi.", textAlign = TextAlign.Center, color = ColorTextSecondary)

        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onScanClick,
            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Icon(Icons.Rounded.Bluetooth, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Quét Bluetooth", fontSize = 16.sp)
        }
    }
}

@Composable
fun ConnectionLoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(color = ColorPrimary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Đang kết nối...", color = ColorTextSecondary, fontSize = 18.sp)
    }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(Icons.Default.Warning, null, tint = ColorDanger, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Đã xảy ra lỗi", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ColorDanger)
        Text(message, color = ColorTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)) {
            Text("Thử lại")
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
        Text("Chọn thiết bị", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        // Danh sách cuộn được bên trong BottomSheet
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (devices.isEmpty()) {
                item {
                    Text("Đang tìm thiết bị...", modifier = Modifier.padding(16.dp), color = ColorTextSecondary)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ColorPrimary)
                }
            }
            items(devices.size) { index ->
                val device = devices[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onDeviceSelected(device) },
                    colors = CardDefaults.cardColors(containerColor = ColorBackground),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Bluetooth, null, tint = ColorPrimary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(device.name ?: "Unknown Device", fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                            Text(device.address, fontSize = 12.sp, color = ColorTextSecondary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onScanAgain,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
        ) {
            Text("Quét Lại")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Helper class để return 4 giá trị
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)