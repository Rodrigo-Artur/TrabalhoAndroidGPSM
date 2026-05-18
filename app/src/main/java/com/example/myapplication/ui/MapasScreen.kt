package com.example.myapplication.ui

import android.preference.PreferenceManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication.data.Contacto
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapaScreen(contactos: List<Contacto>) {
    val context = LocalContext.current

    // Configuração obrigatória do OSMDroid para evitar bloqueios
    remember {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        // Define um "User Agent" para a aplicação (boa prática do OpenStreetMap)
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Usamos o AndroidView para embutir o mapa clássico no Jetpack Compose
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                // Configura o estilo do mapa (Visualização Padrão)
                setTileSource(TileSourceFactory.MAPNIK)
                
                // Permite fazer zoom com os dois dedos (pinça)
                setMultiTouchControls(true)
                
                // Posição inicial e Zoom
                controller.setZoom(12.0)
                controller.setCenter(GeoPoint(-23.5505, -46.6333)) // Exemplo: São Paulo (ajuste conforme quiser)
            }
        },
        update = { mapView ->
            // Limpa marcadores antigos para não duplicar se a lista for atualizada
            mapView.overlays.clear()

            // Percorre os seus contactos e adiciona um pino para cada um
            contactos.forEach { contacto ->
                if (contacto.latitude != null && contacto.longitude != null) {
                    val marker = Marker(mapView)
                    marker.position = GeoPoint(contacto.latitude, contacto.longitude)
                    marker.title = contacto.nome
                    marker.snippet = "Tel: ${contacto.telefone}"
                    
                    // Adiciona o pino ao mapa
                    mapView.overlays.add(marker)
                }
            }
            
            // Força o mapa a desenhar os novos pinos
            mapView.invalidate()
        }
    )
}