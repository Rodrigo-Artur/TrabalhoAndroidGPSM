package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.myapplication.data.Contacto
import com.example.myapplication.utils.MapUtils

@Composable
fun MapaScreen(contactos: List<Contacto>) {
    val context = LocalContext.current
    
    // Posição inicial da câmara (Pode ajustar para pegar o GPS atual do utilizador)
    val lisboa = LatLng(38.7223, -9.1393) 
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(lisboa, 10f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true) // Necessita de permissão ativa
        ) {
            contactos.forEach { contacto ->
                if (contacto.latitude != null && contacto.longitude != null) {
                    val location = LatLng(contacto.latitude, contacto.longitude)
                    
                    MarkerInfoWindowContent(
                        state = MarkerState(position = location),
                        title = contacto.nome,
                        snippet = contacto.telefone
                    ) { marker ->
                        // Balão de informação customizado ao clicar no marcador
                        Card {
                            Column(Modifier.padding(8.dp)) {
                                Text(contacto.nome, style = MaterialTheme.typography.titleMedium)
                                Text("Tel: ${contacto.telefone}")
                                Button(onClick = { 
                                    MapUtils.abrirRotaNoGoogleMaps(context, contacto.latitude, contacto.longitude) 
                                }) {
                                    Text("Ir até contacto")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}