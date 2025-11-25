package com.ptit.iot.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptit.iot.ProfileDto
import com.ptit.iot.ProfileResponse
import com.ptit.iot.onError
import com.ptit.iot.onLoading
import com.ptit.iot.onSuccess
import com.ptit.iot.safeCollectFlow
import com.ptit.iot.viewmodel.ProfileViewModel

@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    
    var isLoading by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    var profileData by remember { mutableStateOf<ProfileResponse.Data?>(null) }
    
    // Form state
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableIntStateOf(0) }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var smoking by remember { mutableIntStateOf(0) }
    var alcohol by remember { mutableIntStateOf(0) }
    
    // Error states
    var nameError by remember { mutableStateOf<String?>(null) }
    var ageError by remember { mutableStateOf<String?>(null) }
    var heightError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }

    // Load profile data
    LaunchedEffect(Unit) {
        viewModel.getProfile()
        
        lifecycleOwner.safeCollectFlow(viewModel.profileState) { resource ->
            resource
                .onLoading { isLoading = true }
                .onSuccess { response ->
                    isLoading = false
                    profileData = response.data
                    // Populate form with existing data
                    response.data?.let { data ->
                        name = data.name ?: ""
                        age = data.profile?.age?.toString() ?: ""
                        gender = data.profile?.gender ?: 0
                        height = data.profile?.height?.toString() ?: ""
                        weight = data.profile?.weight?.toString() ?: ""
                        smoking = data.profile?.smoke ?: 0
                        alcohol = data.profile?.alco ?: 0
                    }
                }
                .onError {
                    isLoading = false
                }
        }
    }
    
    // Handle update profile response
    LaunchedEffect(Unit) {
        lifecycleOwner.safeCollectFlow(viewModel.updateProfileState) { resource ->
            resource
                .onLoading { isUpdating = true }
                .onSuccess {
                    isUpdating = false
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
                .onError { error ->
                    isUpdating = false
                    Toast.makeText(context, "Failed to update profile: ${error.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
    
    // Clear update state when screen is opened
    LaunchedEffect(Unit) {
        viewModel.clearUpdateState()
    }

    fun validateAndSave() {
        // Reset errors
        nameError = null
        ageError = null
        heightError = null
        weightError = null
        
        var hasError = false
        
        // Validate name
        if (name.isBlank()) {
            nameError = "Name is required"
            hasError = true
        }
        
        // Validate age
        val ageInt = age.toIntOrNull()
        if (ageInt == null || ageInt < 1 || ageInt > 120) {
            ageError = "Please enter a valid age (1-120)"
            hasError = true
        }
        
        // Validate height
        val heightInt = height.toIntOrNull()
        if (heightInt == null || heightInt < 50 || heightInt > 250) {
            heightError = "Please enter a valid height (50-250 cm)"
            hasError = true
        }
        
        // Validate weight
        val weightInt = weight.toIntOrNull()
        if (weightInt == null || weightInt < 20 || weightInt > 300) {
            weightError = "Please enter a valid weight (20-300 kg)"
            hasError = true
        }
        
        if (!hasError) {
            val updatedProfile = ProfileDto(
                age = ageInt,
                gender = gender,
                height = heightInt,
                weight = weightInt,
                smoke = smoking,
                alco = alcohol
            )
            viewModel.updateProfile(updatedProfile, name)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Custom Compact Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFDF1F32)
                )
            }
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
        }
        
        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Personal Information Section
            EditSection(
                title = "Personal Information",
                icon = Icons.Default.Person
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = null
                    },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AccountCircle, 
                            contentDescription = null,
                            tint = Color(0xFFDF1F32)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
                
            // Basic Information Section
            EditSection(
                title = "Basic Information",
                icon = Icons.Default.Info
            ) {
                OutlinedTextField(
                    value = age,
                    onValueChange = { 
                        age = it.filter { char -> char.isDigit() }
                        ageError = null
                    },
                    label = { Text("Age") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = ageError != null,
                    supportingText = ageError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DateRange, 
                            contentDescription = null,
                            tint = Color(0xFFDF1F32)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Gender Selection
                Text(
                    text = "Gender",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        onClick = { gender = 1 },
                        label = { Text("Male", fontWeight = FontWeight.Medium) },
                        selected = gender == 1,
                        leadingIcon = if (gender == 1) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDF1F32).copy(alpha = 0.1f),
                            selectedLabelColor = Color(0xFFDF1F32),
                            selectedLeadingIconColor = Color(0xFFDF1F32)
                        )
                    )
                    
                    FilterChip(
                        onClick = { gender = 0 },
                        label = { Text("Female", fontWeight = FontWeight.Medium) },
                        selected = gender == 0,
                        leadingIcon = if (gender == 0) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDF1F32).copy(alpha = 0.1f),
                            selectedLabelColor = Color(0xFFDF1F32),
                            selectedLeadingIconColor = Color(0xFFDF1F32)
                        )
                    )
                }
            }
                
            // Physical Stats Section
            EditSection(
                title = "Physical Stats",
                icon = Icons.Default.FitnessCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = { 
                            height = it.filter { char -> char.isDigit() }
                            heightError = null
                        },
                        label = { Text("Height (cm)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = heightError != null,
                        supportingText = heightError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Height, 
                                contentDescription = null,
                                tint = Color(0xFFDF1F32)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { 
                            weight = it.filter { char -> char.isDigit() }
                            weightError = null
                        },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = weightError != null,
                        supportingText = weightError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        leadingIcon = {
                            Icon(
                                Icons.Default.MonitorWeight, 
                                contentDescription = null,
                                tint = Color(0xFFDF1F32)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
                
            // Lifestyle Section
            EditSection(
                title = "Lifestyle",
                icon = Icons.Default.Healing
            ) {
                // Smoking
                Text(
                    text = "Do you smoke?",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        onClick = { smoking = 0 },
                        label = { Text("No", fontWeight = FontWeight.Medium) },
                        selected = smoking == 0,
                        leadingIcon = if (smoking == 0) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.1f),
                            selectedLabelColor = Color(0xFF10B981),
                            selectedLeadingIconColor = Color(0xFF10B981)
                        )
                    )
                    
                    FilterChip(
                        onClick = { smoking = 1 },
                        label = { Text("Yes", fontWeight = FontWeight.Medium) },
                        selected = smoking == 1,
                        leadingIcon = if (smoking == 1) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.1f),
                            selectedLabelColor = Color(0xFFEF4444),
                            selectedLeadingIconColor = Color(0xFFEF4444)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Alcohol
                Text(
                    text = "Do you drink alcohol?",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        onClick = { alcohol = 0 },
                        label = { Text("No", fontWeight = FontWeight.Medium) },
                        selected = alcohol == 0,
                        leadingIcon = if (alcohol == 0) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.1f),
                            selectedLabelColor = Color(0xFF10B981),
                            selectedLeadingIconColor = Color(0xFF10B981)
                        )
                    )
                    
                    FilterChip(
                        onClick = { alcohol = 1 },
                        label = { Text("Yes", fontWeight = FontWeight.Medium) },
                        selected = alcohol == 1,
                        leadingIcon = if (alcohol == 1) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.1f),
                            selectedLabelColor = Color(0xFFEF4444),
                            selectedLeadingIconColor = Color(0xFFEF4444)
                        )
                    )
                }
            }
            
            // Save Button
            Button(
                onClick = { validateAndSave() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDF1F32)
                ),
                enabled = !isUpdating,
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                if (isUpdating) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Saving...",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Save Changes",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            // Bottom padding for better scrolling
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Loading overlay for initial data loading
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFDF1F32),
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
            }
        }
    }
}

@Composable
private fun EditSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFDF1F32),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            }
            
            content()
        }
    }
}