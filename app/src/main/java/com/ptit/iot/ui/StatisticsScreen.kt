package com.ptit.iot.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptit.iot.HeartRateResponse
import com.ptit.iot.ProfileResponse
import com.ptit.iot.Resource
import com.ptit.iot.safeCollectFlow
import com.ptit.iot.viewmodel.StatisticsViewModel
import kotlinx.coroutines.delay

@Composable
fun StatisticsScreen(
    profile: ProfileResponse.Data?,
    viewModel: StatisticsViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var profileData by remember { mutableStateOf<ProfileResponse.Data?>(null) }
    var heartData by remember { mutableStateOf<HeartRateResponse.Data?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
        viewModel.loadHeartRate()
    }

    LaunchedEffect(Unit) {
        lifecycleOwner.safeCollectFlow(viewModel.profileState) { state ->
            if (state is Resource.Success) profileData = state.data
        }
    }

    LaunchedEffect(Unit) {
        lifecycleOwner.safeCollectFlow(viewModel.heartState) { state ->
            when (state) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    heartData = state.data.data
                }
                is Resource.Error -> {}
                Resource.Idle -> {}
            }
        }
    }

    // Auto call API mỗi 1 giây
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            viewModel.loadHeartRate()
        }
    }

    val bpm = heartData?.bpm ?: 0.0
    val heartStatus = getHeartStatus(bpm)
    val statusColor = getHeartStatusColor(heartStatus)
    val statusGradient = Brush.verticalGradient(
        colors = listOf(statusColor, statusColor.copy(alpha = 0.6f))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFD32F2F),
                        Color(0xFFEF5350)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusGradient)
                    .padding(24.dp)
            ) {
                Text(
                    "Sức Khỏe",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Heart Rate Card
                HeartRateCard(bpm, heartStatus, statusColor)

                Spacer(modifier = Modifier.height(20.dp))

                // Profile Card
                if (profileData != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Health Metrics Card
                if (profileData != null && heartData != null) {
                    HealthMetricsCard(profileData!!, heartData!!)
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun HeartRateCard(bpm: Double, status: String, color: Color) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(700)) + scaleIn(tween(700))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Nhịp Tim Hiện Tại",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF757575)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Animated Heart Chart
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.1f),
                                    Color.Transparent
                                ),
                                radius = 150f
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val animateBpm = animateFloatAsState(
                        targetValue = bpm.coerceIn(0.0, 180.0).toFloat() / 180f,
                        animationSpec = tween(1200, easing = FastOutSlowInEasing)
                    )

                    CircularProgressIndicator(
                        progress = animateBpm.value,
                        modifier = Modifier.size(200.dp),
                        strokeWidth = 16.dp,
                        color = color,
                        trackColor = Color(0xFFE8E8E8),
                        strokeCap = StrokeCap.Round
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${bpm.toInt()}",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Text(
                            text = "BPM",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Trạng thái: $status",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
            }
        }
    }
}


@Composable
fun HealthMetricsCard(profile: ProfileResponse.Data, heart: HeartRateResponse.Data) {
    val height = profile.profile?.height ?: 0
    val weight = profile.profile?.weight ?: 0
    val bmi = if (height > 0 && weight > 0) weight / ((height / 100.0) * (height / 100.0)) else 0.0
    val bmiStatus = getBMIStatus(bmi)
    val bmiColor = getBMIStatusColor(bmiStatus)

    val bpm = heart.bpm ?: 0.0
    val heartStatus = getHeartStatus(bpm)
    val heartColor = getHeartStatusColor(heartStatus)

    val overallStatus = getOverallHealthStatus(bmiStatus, heartStatus)
    val overallColor = getOverallHealthColor(overallStatus)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Chỉ Số Sức Khỏe",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // BMI Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Chỉ Số BMI", fontSize = 14.sp, color = Color(0xFF757575))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "%.1f".format(bmi),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = bmiColor
                    )
                }
                Box(
                    modifier = Modifier
                        .background(bmiColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        bmiStatus,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = bmiColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Overall Health Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(overallColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tình Trạng Sức Khỏe", fontSize = 14.sp, color = Color(0xFF757575))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        overallStatus,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = overallColor
                    )
                }
                Box(
                    modifier = Modifier
                        .background(overallColor, RoundedCornerShape(50.dp))
                        .size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        getHealthEmoji(overallStatus),
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricBadge(
                    icon = "❤️",
                    label = "Nhịp Tim",
                    value = "${bpm.toInt()} BPM",
                    status = heartStatus,
                    color = heartColor,
                    modifier = Modifier.weight(1f)
                )
                MetricBadge(
                    icon = "📊",
                    label = "BMI",
                    value = "%.1f".format(bmi),
                    status = bmiStatus,
                    color = bmiColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun InfoTag(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF616161)
        )
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MetricBadge(
    icon: String,
    label: String,
    value: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = Color(0xFF757575))
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(status, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatisticsScreenEnhanced(
    viewModel: StatisticsViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var profileData by remember { mutableStateOf<ProfileResponse.Data?>(null) }
    var heartData by remember { mutableStateOf<HeartRateResponse.Data?>(null) }

    // Load dữ liệu khi mở màn hình
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
        viewModel.loadHeartRate()
    }

    LaunchedEffect(Unit) {
        lifecycleOwner.safeCollectFlow(viewModel.profileState) { state ->
            if (state is Resource.Success) profileData = state.data
        }
    }

    // Collect heart rate
    LaunchedEffect(Unit) {
        lifecycleOwner.safeCollectFlow(viewModel.heartState) { state ->
            when (state) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    heartData = state.data.data
                }
                is Resource.Error -> {}
                Resource.Idle -> {}
            }
        }
    }

    // Auto call API mỗi 1 giây - Real time
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            viewModel.loadHeartRate()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFD32F2F),  // đỏ đậm
                        Color(0xFFEF5350)   // đỏ nhạt
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "📊 Thống Kê Sức Khỏe",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6F00)
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (heartData != null) {
                HeartRateCard(
                    bpm = heartData!!.bpm ?: 0.0,
                    status = getHeartStatus(heartData!!.bpm ?: 0.0),
                    color = getHeartStatusColor(getHeartStatus(heartData!!.bpm ?: 0.0))
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (profileData != null) {
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (profileData != null && heartData != null) {
                HealthMetricsCard(profileData!!, heartData!!)
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (profileData != null && heartData != null) {
                HealthAnalysisCard(profileData!!, heartData!!)
                Spacer(modifier = Modifier.height(20.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

}

@Composable
fun HealthAnalysisCard(profile: ProfileResponse.Data, heart: HeartRateResponse.Data) {
    val height = profile.profile?.height ?: 0
    val weight = profile.profile?.weight ?: 0
    val bpm = heart.bpm ?: 0.0

    val adviceList = mutableListOf<String>()
    val analysisColor: Color

    // Phân tích chiều cao / nhịp tim
    if (height > 0) {
        val bpmHeightRatio = bpm / height
        if (bpmHeightRatio < 0.5) adviceList.add("Nhịp tim hơi thấp so với chiều cao")
        else if (bpmHeightRatio > 1.0) adviceList.add("Nhịp tim cao so với chiều cao")
        else adviceList.add("Nhịp tim phù hợp với chiều cao")
    }

    // Phân tích cân nặng / nhịp tim
    if (weight > 0) {
        val bpmWeightRatio = bpm / weight
        if (bpmWeightRatio < 0.8) adviceList.add("Nhịp tim thấp so với cân nặng")
        else if (bpmWeightRatio > 1.5) adviceList.add("Nhịp tim cao so với cân nặng")
        else adviceList.add("Nhịp tim phù hợp với cân nặng")
    }

    // Tổng quan để chọn màu
    analysisColor = if (bpm < 60 || bpm > 100) Color(0xFFFF5252) else Color(0xFF43A047)

    // Gợi ý dinh dưỡng & sinh hoạt
    adviceList.add("Hãy ăn nhiều rau xanh, trái cây, uống đủ nước và tập thể dục nhẹ nhàng")
    adviceList.add("Ngủ đủ giấc và tránh căng thẳng để duy trì nhịp tim ổn định")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Phân Tích Chiều Cao & Cân Nặng", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = analysisColor)
            Spacer(modifier = Modifier.height(12.dp))
            adviceList.forEach { advice ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("• ", fontSize = 14.sp, color = Color(0xFF757575))
                    Text(advice, fontSize = 14.sp, color = Color(0xFF424242))
                }
            }
        }
    }
}

fun getHeartStatus(bpm: Double): String = when {
    bpm < 60 -> "Thấp"
    bpm in 60.0..100.0 -> "Bình thường"
    else -> "Cao"
}

fun getHeartStatusColor(status: String): Color = when (status) {
    "Thấp" -> Color(0xFF1E88E5)
    "Bình thường" -> Color(0xFF43A047)
    else -> Color(0xFFFF5252)
}

fun getBMIStatus(bmi: Double): String = when {
    bmi < 18.5 -> "Thiếu cân"
    bmi in 18.5..24.9 -> "Bình thường"
    bmi in 25.0..29.9 -> "Thừa cân"
    else -> "Béo phì"
}

fun getBMIStatusColor(status: String): Color = when (status) {
    "Thiếu cân" -> Color(0xFFFF9800)
    "Bình thường" -> Color(0xFF43A047)
    "Thừa cân" -> Color(0xFFFFA726)
    else -> Color(0xFFFF5252)
}

fun getOverallHealthStatus(bmiStatus: String, heartStatus: String): String = when {
    heartStatus == "Cao" || bmiStatus == "Béo phì" -> "Cảnh báo cao"
    heartStatus == "Thấp" || bmiStatus == "Thiếu cân" -> "Cần chú ý"
    else -> "Bình thường"
}

fun getOverallHealthColor(status: String): Color = when (status) {
    "Bình thường" -> Color(0xFF43A047)
    "Cần chú ý" -> Color(0xFFFFA726)
    else -> Color(0xFFFF5252)
}

fun getHealthEmoji(status: String): String = when (status) {
    "Bình thường" -> "✅"
    "Cần chú ý" -> "⚠️"
    else -> "🔴"
}