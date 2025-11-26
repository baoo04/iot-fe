// WorkoutScreen.kt
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
import androidx.compose.ui.draw.scale
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
import com.ptit.iot.safeCollectFlow
import com.ptit.iot.viewmodel.WorkoutStats
import com.ptit.iot.viewmodel.WorkoutViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val isWorkoutActive by viewModel.isWorkoutActive.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val workoutStats by viewModel.workoutStats.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF6F00),
                        Color(0xFFFF9100)
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
            // Header
            Text(
                text = "💪 Workout",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 20.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            if (!isWorkoutActive) {
                // Pre-workout screen
                PreWorkoutContent(viewModel)
            } else {
                // Active workout screen
                ActiveWorkoutContent(
                    elapsedTime = elapsedTime,
                    workoutStats = workoutStats,
                    viewModel = viewModel
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PreWorkoutContent(viewModel: WorkoutViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .size(200.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏃",
                    fontSize = 80.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Sẵn sàng bắt đầu?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Nhấn nút Start để bắt đầu tập thể dục",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { viewModel.startWorkout() },
            modifier = Modifier
                .width(150.dp)
                .height(56.dp)
                .shadow(8.dp, RoundedCornerShape(50.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(50.dp)
        ) {
            Text(
                text = "START",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6F00)
            )
        }
    }
}

@Composable
fun ActiveWorkoutContent(
    elapsedTime: Long,
    workoutStats: WorkoutStats,
    viewModel: WorkoutViewModel
) {
    val minutes = (elapsedTime / 1000 / 60).toInt()
    val seconds = (elapsedTime / 1000 % 60).toInt()
    val currentBpm = workoutStats.bpmReadings.lastOrNull() ?: 0.0

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Timer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Thời gian",
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "%02d:%02d".format(minutes, seconds),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6F00),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Current Heart Rate
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nhịp Tim Hiện Tại",
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val scale = remember { Animatable(1f) }
                    LaunchedEffect(currentBpm) {
                        scale.animateTo(1.1f, animationSpec = tween(150))
                        scale.animateTo(1f, animationSpec = tween(150))
                    }

                    CircularProgressIndicator(
                        progress = currentBpm.coerceIn(0.0, 180.0).toFloat() / 180f,
                        modifier = Modifier
                            .size(150.dp)
                            .scale(scale.value),
                        strokeWidth = 12.dp,
                        color = Color(0xFFFF6F00),
                        trackColor = Color(0xFFE8E8E8),
                        strokeCap = StrokeCap.Round
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentBpm.toInt().toString(),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6F00)
                        )
                        Text(
                            text = "BPM",
                            fontSize = 12.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Statistics (Hiển thị khi có đủ dữ liệu)
        if (workoutStats.bpmReadings.isNotEmpty()) {
            WorkoutStatsCard(workoutStats)
            Spacer(modifier = Modifier.height(20.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Control Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.stopWorkout() },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "STOP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6F00)
                )
            }
        }
    }
}

@Composable
fun WorkoutStatsCard(stats: WorkoutStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "📊 Thống Kê",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Average BPM
            WorkoutStatRow(
                icon = "❤️",
                label = "Nhịp Tim Trung Bình",
                value = "%.0f BPM".format(stats.avgBpm)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Max BPM
            WorkoutStatRow(
                icon = "📈",
                label = "Nhịp Tim Tối Đa",
                value = "%.0f BPM".format(stats.maxBpm)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Min BPM
            WorkoutStatRow(
                icon = "📉",
                label = "Nhịp Tim Tối Thiểu",
                value = "%.0f BPM".format(stats.minBpm)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Calories
            WorkoutStatRow(
                icon = "🔥",
                label = "Calo Tiêu Hao",
                value = "%.1f kcal".format(stats.caloriesBurned)
            )
        }
    }
}

@Composable
fun WorkoutStatRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 18.sp)
            Text(
                label,
                fontSize = 14.sp,
                color = Color(0xFF616161),
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF6F00)
        )
    }
}