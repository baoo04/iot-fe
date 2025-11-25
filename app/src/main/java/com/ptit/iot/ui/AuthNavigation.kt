package com.ptit.iot.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class AuthScreen(val route: String) {
    data object Login : AuthScreen("login")
    data object Register : AuthScreen("register")
}

@Composable
fun AuthNavigation(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AuthScreen.Login.route,
        modifier = modifier
    ) {
        composable(AuthScreen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(AuthScreen.Register.route)
                }
            )
        }
        
        composable(AuthScreen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
    }
}
