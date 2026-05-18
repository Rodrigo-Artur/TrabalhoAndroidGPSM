package com.example.myapplication

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.ui.ContactosScreen
import com.example.myapplication.ui.MapaScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

        val db = AppDatabase.getDatabase(this)
        val contactoDao = db.contactoDao()

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.List, contentDescription = "Agenda") },
                                label = { Text("Agenda") },
                                selected = currentRoute == "lista",
                                onClick = {
                                    navController.navigate("lista") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Map, contentDescription = "Mapa") },
                                label = { Text("Mapa") },
                                selected = currentRoute == "mapa",
                                onClick = {
                                    navController.navigate("mapa") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    // O NavHost agora respeita o padding da BottomBar
                    NavHost(
                        navController = navController, 
                        startDestination = "lista",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("lista") {
                            ContactosScreen(contactoDao = contactoDao)
                        }
                        composable("mapa") {
                            val contactos by contactoDao.listarContactos().collectAsState(initial = emptyList())
                            MapaScreen(contactos = contactos)
                        }
                    }
                }
            }
        }
    }
}