package com.ptit.iot.ui.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
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
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import kotlin.math.sqrt

// --- BẢNG MÀU GIAO DIỆN CŨ (THEME CAM) ---
val ThemeOrange = Color(0xFFEC1860)       // Cam đậm chủ đạo
val ThemeOrangeLight = Color(0xFFFFCCBC)  // Cam nhạt (nền phụ)
val ThemeBackground = Color(0xFFFAFAFA)   // Nền trắng xám
val ThemeSurface = Color(0xFFFFFFFF)      // Trắng tinh (Card)
val ColorSafe = Color(0xFF4CAF50)         // Xanh lá (An toàn)
val ColorWarning = Color(0xFFFFC107)      // Vàng (Cảnh báo nhẹ)
val ColorDanger = Color(0xFFD32F2F)       // Đỏ (Nguy hiểm)
val TextDark = Color(0xFF212121)          // Chữ đen
val TextLight = Color(0xFF757575)         // Chữ xám

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

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    val bluetoothScanner = remember { BluetoothScanner(context) }

    LaunchedEffect(userId) {
        userId?.let { viewModel.setUserId(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Health Monitor ESP32",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ThemeOrange
                ),
                modifier = Modifier.shadow(8.dp)
            )
        },
        containerColor = ThemeBackground
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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
                    is ConnectionState.Connecting -> ConnectionLoadingContent()
                    is ConnectionState.Connected -> {
                        ConnectedContent(
                            heartRateState = heartRateState,
                            trainState = trainState,
                            onDisconnect = { viewModel.disconnect() },
                            onTrainModel = { viewModel.trainModel() }
                        )
                    }
                    is ConnectionState.Error -> {
                        ErrorContent((connectionState as ConnectionState.Error).message) {
                            viewModel.disconnect()
                        }
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
                containerColor = ThemeSurface
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

// --- GIAO DIỆN CHÍNH KHI ĐÃ KẾT NỐI ---
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

            // 1. THANH CẢNH BÁO (THAY THẾ LOA)
            StatusBanner(warningCode = data.warning)

            Spacer(modifier = Modifier.height(24.dp))

            // 2. KHỐI NHỊP TIM TRUNG TÂM
            HeartRateDisplay(
                bpm = data.currentBpm,
                min = data.minBpm,
                max = data.maxBpm,
                avg = data.avgBpm
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. GRID THÔNG SỐ (SpO2 & BƯỚC CHÂN)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Thẻ SpO2
                InfoCard(
                    title = "Nồng độ Oxy",
                    value = "${data.spo2}",
                    unit = "%",
                    iconId = R.drawable.main_heart, // Thay icon phổi nếu có
                    color = if(data.spo2 >= 95) ColorSafe else ColorWarning,
                    modifier = Modifier.weight(1f)
                )

                // Thẻ Bước Chân
                InfoCard(
                    title = "Bước chân",
                    value = "${data.steps}",
                    unit = "bước",
                    iconVector = Icons.Rounded.DirectionsRun,
                    color = Color.Blue,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. MÔI TRƯỜNG & GIA TỐC
            EnvironmentCard(data.tempMpu, data.accX, data.accY, data.accZ)

            Spacer(modifier = Modifier.height(24.dp))

            // 5. NÚT TRAIN MODEL
            TrainSection(trainState, onTrainModel)

            Spacer(modifier = Modifier.height(32.dp))

            // 6. NÚT NGẮT KẾT NỐI
            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, ThemeOrange),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeOrange)
            ) {
                Text("NGẮT KẾT NỐI", fontWeight = FontWeight.Bold)
            }
        }
        is HeartRateState.NoData, is HeartRateState.Loading -> {
            LoadingView(onDisconnect)
        }
        is HeartRateState.Error -> {
            ErrorContent((heartRateState as HeartRateState.Error).message, onDisconnect)
        }
    }
}

// --- CÁC COMPONENT CON ---

@Composable
fun StatusBanner(warningCode: Int) {
    // Logic màu sắc và chữ dựa trên warning code
    val (bgColor, textColor, mainText, subText) = when (warningCode) {
        0 -> Quadruple(ColorSafe, Color.White, "BÌNH THƯỜNG", "Nhịp tim ổn định")
        1 -> Quadruple(ColorWarning, TextDark, "CẢNH BÁO", "Nhịp tim hơi cao")
        else -> Quadruple(ColorDanger, Color.White, "NGUY HIỂM", "Nhịp tim bất thường!")
    }

    // Hiệu ứng nhấp nháy nếu là Nguy Hiểm
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (warningCode > 1) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }, // Áp dụng nhấp nháy
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = mainText,
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                )
                Text(
                    text = subText,
                    style = TextStyle(fontSize = 14.sp, color = textColor.copy(alpha = 0.9f))
                )
            }
        }
    }
}

@Composable
fun HeartRateDisplay(bpm: Int, min: Int, max: Int, avg: Int) {
    // Animation Pulse
    val pulseScale by animateFloatAsState(
        targetValue = if (bpm > 0) 1.1f else 1f,
        animationSpec = tween(500)
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Vòng tròn trung tâm
        Box(
            modifier = Modifier
                .size(200.dp)
                .shadow(16.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF8A65), ThemeOrange) // Gradient Cam
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.main_heart),
                contentDescription = null,
                modifier = Modifier.size(120.dp).scale(pulseScale)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = 10.dp)) {
                Text(
                    text = "$bpm",
                    style = TextStyle(fontSize = 52.sp, fontWeight = FontWeight.Black, color = Color.White)
                )
                Text("BPM", style = TextStyle(fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f)))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Thanh thống kê nhỏ
        Card(
            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Min", "$min")
                StatItem("Avg", "$avg")
                StatItem("Max", "$max")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ThemeOrange)
        Text(label, fontSize = 12.sp, color = TextLight)
    }
}

@Composable
fun InfoCard(
    title: String,
    value: String,
    unit: String,
    iconId: Int? = null,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (iconId != null) {
                    Icon(painter = painterResource(iconId), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                } else if (iconVector != null) {
                    Icon(imageVector = iconVector, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 14.sp, color = TextLight, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, fontSize = 14.sp, color = TextLight, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
fun EnvironmentCard(temp: Double, accX: Double, accY: Double, accZ: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Thermostat, null, tint = ThemeOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cảm biến MPU6050", fontWeight = FontWeight.Bold, color = TextDark)
            }
            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.2f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Nhiệt độ", fontSize = 12.sp, color = TextLight)
                    Text("${String.format("%.1f", temp)}°C", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ThemeOrange)
                }

                val totalG = sqrt(accX*accX + accY*accY + accZ*accZ)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Gia tốc", fontSize = 12.sp, color = TextLight)
                    Text("${String.format("%.2f", totalG)} m/s²", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
            }
        }
    }
}

@Composable
fun TrainSection(state: TrainState, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = ThemeSurface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Huấn luyện AI", fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(12.dp))
            if (state is TrainState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ThemeOrange)
            } else {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeOrange),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("CẬP NHẬT MODEL")
                }
            }
            if (state is TrainState.Success) Text("Thành công!", color = ColorSafe, fontSize = 12.sp, modifier = Modifier.padding(top=8.dp))
            if (state is TrainState.Error) Text(state.message, color = ColorDanger, fontSize = 12.sp, modifier = Modifier.padding(top=8.dp))
        }
    }
}

@Composable
fun LoadingView(onDisconnect: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 40.dp)) {
        CircularProgressIndicator(color = ThemeOrange)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Đang đồng bộ dữ liệu...", color = TextLight)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onDisconnect, colors = ButtonDefaults.buttonColors(containerColor = TextLight)) { Text("Hủy") }
    }
}

// --- MÀN HÌNH CHƯA KẾT NỐI & SCAN (Giữ nguyên logic, đổi màu) ---

@Composable
fun EmptyStateContent(onScanClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 60.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.main_heart),
            contentDescription = null,
            modifier = Modifier.size(100.dp).graphicsLayer { alpha = 0.5f }
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Chưa kết nối thiết bị", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Vui lòng kết nối để bắt đầu", color = TextLight)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onScanClick,
            colors = ButtonDefaults.buttonColors(containerColor = ThemeOrange),
            modifier = Modifier.height(50.dp)
        ) {
            Icon(Icons.Rounded.Bluetooth, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("QUÉT THIẾT BỊ")
        }
    }
}

@Composable
fun ConnectionLoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ThemeOrange)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Đang kết nối...", color = TextLight)
        }
    }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 50.dp)) {
        Icon(Icons.Default.Warning, null, tint = ColorDanger, modifier = Modifier.size(50.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Lỗi kết nối", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(message, color = TextLight, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = ThemeOrange)) { Text("Thử lại") }
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
        Text("Chọn Thiết Bị", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (devices.isEmpty()) {
                item {
                    Text("Đang quét...", color = TextLight)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ThemeOrange)
                }
            }
            items(devices.size) { index ->
                val device = devices[index]
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onDeviceSelected(device) },
                    colors = CardDefaults.cardColors(containerColor = ThemeBackground),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Bluetooth, null, tint = ThemeOrange)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(device.name ?: "Không tên", fontWeight = FontWeight.Bold)
                            Text(device.address, fontSize = 12.sp, color = TextLight)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onScanAgain,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ThemeOrange)
        ) { Text("QUÉT LẠI") }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)