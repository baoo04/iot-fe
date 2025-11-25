package com.ptit.iot.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptit.iot.MainActivity
import com.ptit.iot.R
import com.ptit.iot.Resource
import com.ptit.iot.safeCollectFlow
import com.ptit.iot.viewmodel.LoginViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var email by remember { mutableStateOf("test@yopmail.com") }
    var password by remember { mutableStateOf("Test123$") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Define colors based on Figma design
    val primaryRed = Color(0xFFDF1F32)
    val inputBackground = Color(0x7AFED1D2) // rgba(254, 209, 210, 0.48)
    val inputBorder = Color(0xFFFA743E)
    
    LaunchedEffect(Unit) {
        lifecycleOwner.safeCollectFlow(viewModel.loginState) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    isLoading = true
                }
                is Resource.Success -> {
                    isLoading = false
                    val intent = Intent(context, MainActivity::class.java)
                    // Pass user data if needed
                    resource.data.data?.let { userData ->
                        intent.putExtra("USER_ID", userData.userId)
                    }
                    context.startActivity(intent)
                }
                is Resource.Error -> {
                    isLoading = false
                    Toast.makeText(
                        context,
                        "Error: ${resource.error.message ?: "Unknown error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    isLoading = false
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status bar space
        Spacer(modifier = Modifier.height(48.dp))
        
        // Heart logo
        Image(
            painter = painterResource(id = R.drawable.heart),
            contentDescription = "Heart logo",
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 24.dp)
        )
        
        // Login title
        Text(
            text = "Đăng nhập",
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(bottom = 40.dp)
        )
        
        // Email field label
        Text(
            text = "Email",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            ),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )
        
        // Email input field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = inputBackground,
                    shape = RoundedCornerShape(10.dp)
                ),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = inputBorder,
                unfocusedBorderColor = inputBorder,
                cursorColor = Color.Black
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password field label
        Text(
            text = "Mật khẩu",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            ),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )
        
        // Password input field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = inputBackground,
                    shape = RoundedCornerShape(10.dp)
                ),
            shape = RoundedCornerShape(10.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = inputBorder,
                unfocusedBorderColor = inputBorder,
                cursorColor = Color.Black
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val iconRes = if (passwordVisible) R.drawable.ic_visibility_on else R.drawable.ic_visibility_off
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Login button
        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    viewModel.login(email, password)
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryRed
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White, 
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Đăng nhập",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.1.sp
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Hoặc",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Chưa có tài khoản ?   ",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.1.sp
                )
            )
            
            TextButton(
                onClick = { onNavigateToRegister() },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Đăng ký ngay",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.1.sp,
                        color = primaryRed
                    )
                )
            }
        }
    }
}
