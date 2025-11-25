package com.ptit.iot.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptit.iot.ProfileResponse
import com.ptit.iot.onError
import com.ptit.iot.onLoading
import com.ptit.iot.onSuccess
import com.ptit.iot.safeCollectFlow
import com.ptit.iot.viewmodel.ProfileViewModel
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit = {},
) {
    val viewModel: ProfileViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current

    var isLoading by remember { mutableStateOf(false) }
    var profileData by remember { mutableStateOf<ProfileResponse.Data?>(null) }

    LaunchedEffect(Unit) {
        viewModel.getProfile()
        
        lifecycleOwner.safeCollectFlow(viewModel.profileState) { resource ->
            resource
                .onLoading { isLoading = true }
                .onSuccess { response ->
                    isLoading = false
                    profileData = response.data
                }
                .onError {
                    isLoading = false
                    profileData = null
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFDF1F32),
                                Color(0xFFFF6B6B)
                            )
                        )
                    )
                    .padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Avatar with improved styling
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(
                                width = 3.dp,
                                color = Color.White.copy(alpha = 0.5f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Avatar",
                            modifier = Modifier.size(50.dp),
                            tint = Color.White
                        )
                    }
                    
                    // User Info centered with better spacing
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = profileData?.name ?: "Unknown User",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = profileData?.email ?: "No email",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // Single Edit Profile Button
                    ElevatedButton(
                        onClick = onEditProfile,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 4.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit Profile",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // BMI Card (if height and weight available)
                profileData?.profile?.let { profile ->
                    if (profile.height != null && profile.weight != null) {
                        BMICard(
                            height = profile.height,
                            weight = profile.weight
                        )
                    }
                }
                
                // Basic Info Section
                InfoSection(
                    title = "Basic Information",
                    icon = Icons.Default.Info
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoCard(
                            icon = Icons.Default.DateRange,
                            label = "Age",
                            value = "${profileData?.profile?.age ?: "-"}",
                            modifier = Modifier.weight(1f)
                        )
                        
                        InfoCard(
                            icon = if (profileData?.profile?.gender == 1) Icons.Default.Person else Icons.Default.Person,
                            label = "Gender",
                            value = when (profileData?.profile?.gender) {
                                1 -> "Male"
                                0 -> "Female"
                                else -> "-"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Physical Stats Section
                InfoSection(
                    title = "Physical Stats",
                    icon = Icons.Default.FitnessCenter
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoCard(
                            icon = Icons.Default.Height,
                            label = "Height",
                            value = "${profileData?.profile?.height ?: "-"} cm",
                            modifier = Modifier.weight(1f)
                        )
                        
                        InfoCard(
                            icon = Icons.Default.MonitorWeight,
                            label = "Weight",
                            value = "${profileData?.profile?.weight ?: "-"} kg",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Lifestyle Section
                InfoSection(
                    title = "Lifestyle",
                    icon = Icons.Default.Healing
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoCard(
                            icon = Icons.Default.SmokingRooms,
                            label = "Smoking",
                            value = when (profileData?.profile?.smoke) {
                                1 -> "Yes"
                                0 -> "No"
                                else -> "-"
                            },
                            modifier = Modifier.weight(1f),
                            valueColor = if (profileData?.profile?.smoke == 1) 
                                Color(0xFFE53E3E) else 
                                Color(0xFFDF1F32)
                        )
                        
                        InfoCard(
                            icon = Icons.Default.LocalBar,
                            label = "Alcohol",
                            value = when (profileData?.profile?.alco) {
                                1 -> "Yes"
                                0 -> "No"
                                else -> "-"
                            },
                            modifier = Modifier.weight(1f),
                            valueColor = if (profileData?.profile?.alco == 1) 
                                Color(0xFFE53E3E) else 
                                Color(0xFFDF1F32)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFDF1F32)
                )
            }
        }
    }
}

@Composable
private fun BMICard(
    height: Int,
    weight: Int,
    modifier: Modifier = Modifier
) {
    val heightInMeters = height / 100.0
    val bmi = weight / (heightInMeters.pow(2))
    val bmiCategory = when {
        bmi < 18.5 -> Triple("Underweight", Color(0xFF3B82F6), 0.3f)
        bmi < 25 -> Triple("Normal", Color(0xFF10B981), 0.6f)
        bmi < 30 -> Triple("Overweight", Color(0xFFF59E0B), 0.8f)
        else -> Triple("Obese", Color(0xFFEF4444), 1.0f)
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // BMI Circle Progress
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val strokeWidth = 12.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    // Background circle
                    drawCircle(
                        color = Color(0xFFE5E7EB),
                        radius = radius,
                        center = center,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Progress arc
                    val sweepAngle = 270f * bmiCategory.third
                    drawArc(
                        color = bmiCategory.second,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(
                            center.x - radius,
                            center.y - radius
                        ),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }
                
                Text(
                    text = "${bmi.roundToInt()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = bmiCategory.second
                )
            }
            
            // BMI Info
            Column(
                modifier = Modifier.weight(1f).padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "BMI Index",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
                
                Text(
                    text = bmiCategory.first,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = bmiCategory.second
                )
                
                Text(
                    text = "Height: ${height}cm • Weight: ${weight}kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFDF1F32),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            content()
        }
    }
}


@Composable
private fun InfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color(0xFF1F2937)
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFDF1F32),
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = valueColor
            )
        }
    }
}