// WorkoutScreen.kt
package com.ptit.iot.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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
    val showPauseDialog by viewModel.showPauseDialog.collectAsState()
    val pauseCountdown by viewModel.pauseCountdown.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

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
                if (workoutStats.isWorkoutCompleted) {
                    // Màn hình hoàn thành tập luyện
                    WorkoutCompletedContent(stats = workoutStats, viewModel = viewModel)
                } else {
                    // Màn hình trước khi bắt đầu
                    PreWorkoutContent(viewModel)
                }
            } else {
                // Màn hình tập luyện đang diễn ra
                ActiveWorkoutContent(
                    elapsedTime = elapsedTime,
                    workoutStats = workoutStats,
                    viewModel = viewModel
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Dialog thông báo tạm dừng
        if (showPauseDialog) {
            PauseDialogContent(
                countdown = pauseCountdown,
                onResume = { viewModel.resumeWorkout() },
                onReset = { viewModel.resetWorkout() }
            )
        }
    }
}

@Composable
fun PreWorkoutContent(viewModel: WorkoutViewModel) {
    var distanceInput by remember { mutableStateOf("") }

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
            text = "Nhập quãng đường cần chạy (km)",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Input field cho khoảng cách
        TextField(
            value = distanceInput,
            onValueChange = { distanceInput = it },
            modifier = Modifier
                .width(150.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            placeholder = {
                Text("Ví dụ: 5.0", textAlign = TextAlign.Center)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color(0xFFFF6F00),
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                val distance = distanceInput.toDoubleOrNull() ?: 0.0
                if (distance > 0) {
                    viewModel.startWorkout(distance)
                }
            },
            enabled = distanceInput.toDoubleOrNull() ?: 0.0 > 0,
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
    val progressPercentage = if (workoutStats.targetDistance > 0) {
        (workoutStats.currentDistance / workoutStats.targetDistance).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }

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

        // Steps and Distance Progress
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
                    text = "👟 Tiến độ chạy",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF757575)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Distance progress bar
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Quãng đường",
                            fontSize = 12.sp,
                            color = Color(0xFF616161)
                        )
                        Text(
                            "%.2f / %.2f km".format(
                                workoutStats.currentDistance,
                                workoutStats.targetDistance
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6F00)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = progressPercentage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFFF6F00),
                        trackColor = Color(0xFFE8E8E8)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Steps
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Số bước chân", fontSize = 14.sp, color = Color(0xFF616161))
                    Text(
                        workoutStats.stepCount.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6F00)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Statistics
        if (workoutStats.bpmReadings.isNotEmpty()) {

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
                onClick = { viewModel.pauseWorkout() },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "PAUSE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6F00)
                )
            }

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
fun WorkoutCompletedContent(stats: WorkoutStats, viewModel: WorkoutViewModel) {
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
                    text = "🎉",
                    fontSize = 80.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Chúc mừng!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Bạn đã hoàn thành mục tiêu",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))



        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = { viewModel.resetWorkout() },
            modifier = Modifier
                .width(150.dp)
                .height(56.dp)
                .shadow(8.dp, RoundedCornerShape(50.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(50.dp)
        ) {
            Text(
                text = "TRỞ LẠI",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6F00)
            )
        }
    }
}

@Composable
fun PauseDialogContent(
    countdown: Int,
    onResume: () -> Unit,
    onReset: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                "Tạm dừng thử thách",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Bạn có $countdown giây để tiếp tục",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    countdown.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6F00)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6F00))
            ) {
                Text("Tiếp tục", color = Color.White)
            }
        },
        dismissButton = {
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(4.dp))
            ) {
                Text("Restart", color = Color(0xFFFF6F00))
            }
        },
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
    )
}
