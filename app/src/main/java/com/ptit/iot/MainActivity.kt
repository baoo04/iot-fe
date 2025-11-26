package com.ptit.iot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ptit.iot.ui.StatisticsScreenEnhanced
import com.ptit.iot.ui.EditProfileScreen
import com.ptit.iot.ui.ProfileScreen
import com.ptit.iot.ui.main.MainScreen
import com.ptit.iot.ui.theme.IOTHeartRateTheme
import com.ptit.iot.ui.WorkoutScreen
import kotlinx.serialization.Serializable
import com.ptit.iot.viewmodel.StatisticsViewModel

class MainActivity : ComponentActivity() {
    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permission result handled in UI */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request necessary permissions
        requestBluetoothPermissions()

        enableEdgeToEdge()
        setContent {
            IOTHeartRateTheme {
                MainNavigationApp(userId = intent.getStringExtra("USER_ID"))
            }
        }
    }

    private fun requestBluetoothPermissions() {
        val permissionsToRequest = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }
}

@Serializable
object HomeRoute

@Serializable
object WorkoutRoute

@Serializable
object StaitisticsRoute

@Serializable
object ProfileRoute

@Serializable
object EditProfileRoute

data class NavigationItem(
    val route: Any,
    val icon: ImageVector,
    val label: String,
    val contentDescription: String,
)

@Composable
fun MainNavigationApp(userId: String?) {
    val navController = rememberNavController()

    val navigationItems = listOf(
        NavigationItem(
            route = HomeRoute,
            icon = Icons.Outlined.FavoriteBorder,
            label = "Home",
            contentDescription = "Home"
        ),
        NavigationItem(
            route = StaitisticsRoute,
            icon = Icons.AutoMirrored.Outlined.ShowChart,
            label = "Statistics",
            contentDescription = "Statistics"
        ),
        NavigationItem(
            route = WorkoutRoute,
            icon = Icons.Outlined.FitnessCenter,
            label = "Workout",
            contentDescription = "Workout"
        ),
        NavigationItem(
            route = ProfileRoute,
            icon = Icons.Outlined.Person,
            label = "Profile",
            contentDescription = "Profile"
        )
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navigationItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.contentDescription
                            )
                        },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(item.route::class)
                        } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFDF1F32),
                            selectedTextColor = Color(0xFFDF1F32),
                            unselectedIconColor = Color.Black,
                            unselectedTextColor = Color.Black,
                            indicatorColor = Color(0xFFDF1F32).copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<HomeRoute> {
                MainScreen(userId = userId)
            }
            composable<StaitisticsRoute> {
                StatisticsScreenEnhanced()
            }
            composable<WorkoutRoute> {
                WorkoutScreen()
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    onEditProfile = {
                        navController.navigate(EditProfileRoute)
                    }
                )
            }

            composable<EditProfileRoute> {
                EditProfileScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}