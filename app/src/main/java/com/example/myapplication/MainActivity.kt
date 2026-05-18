package com.example.myapplication

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.ui.ContactosScreen
import com.example.myapplication.ui.MapaScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    // Pedido de permissões do Android
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Aqui pode tratar se o utilizador recusou as permissões
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Solicita Permissões de Localização e Notificações (Android 13+)
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

        // Inicializa o Banco de Dados
        val db = AppDatabase.getDatabase(this)
        val contactoDao = db.contactoDao()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "lista") {
                        composable("lista") {
                            ContactosScreen(
                                contactoDao = contactoDao,
                                onNavigateToMap = { navController.navigate("mapa") }
                            )
                        }
                        composable("mapa") {
                            // Carrega a lista para enviar ao mapa
                            val contactos by contactoDao.listarContactos().collectAsState(initial = emptyList())
                            MapaScreen(contactos = contactos)
                        }
                    }
                }
            }
        }
    }
}