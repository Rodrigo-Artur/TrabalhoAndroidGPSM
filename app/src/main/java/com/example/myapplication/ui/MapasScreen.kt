package com.example.myapplication.ui

import android.Manifest
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.data.Contacto
import com.example.myapplication.utils.MapUtils
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapaScreen(contactos: List<Contacto>) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var localizacaoAtual by remember { mutableStateOf<android.location.Location?>(null) }

    // Obter localização atual
    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) localizacaoAtual = loc
            }
        }
    }

    remember {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(14.0)

                // Bolinha azul do GPS
                val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                myLocationOverlay.enableMyLocation()
                overlays.add(myLocationOverlay)
            }
        },
        update = { mapView ->
            mapView.overlays.retainAll { it is MyLocationNewOverlay }

            var contatoMaisProximo: Contacto? = null
            var menorDistancia = Float.MAX_VALUE

            // 1. Descobre quem é o mais próximo
            if (localizacaoAtual != null) {
                contactos.forEach { c ->
                    if (c.latitude != null && c.longitude != null) {
                        val dist = MapUtils.calcularDistancia(
                            localizacaoAtual!!.latitude, localizacaoAtual!!.longitude,
                            c.latitude!!, c.longitude!!
                        )
                        if (dist < menorDistancia) {
                            menorDistancia = dist
                            contatoMaisProximo = c
                        }
                    }
                }
            }

            var markerMaisProximoObj: Marker? = null

            // 2. Adiciona os pinos
            contactos.forEach { contacto ->
                if (contacto.latitude != null && contacto.longitude != null) {
                    val marker = Marker(mapView)
                    marker.position = GeoPoint(contacto.latitude!!, contacto.longitude!!)
                    
                    val isMaisProximo = (contacto.id == contatoMaisProximo?.id)
                    
                    if (isMaisProximo) {
                        // Usa a ESTRELA nativa do sistema Android para destacar!
                        marker.icon = ContextCompat.getDrawable(context, android.R.drawable.btn_star_big_on)
                        marker.title = "⭐ ${contacto.nome}"
                        marker.snippet = "MAIS PRÓXIMO\nTel: ${contacto.telefone}\nClique para rota"
                    } else {
                        // Deixa o OSMDroid usar o pino azul padrão dele
                        marker.title = contacto.nome
                        marker.snippet = "Tel: ${contacto.telefone}\nClique para rota"
                    }
                    
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    // Evento de abrir o GPS de navegação
                    marker.setOnMarkerClickListener { _, _ ->
                        MapUtils.abrirRotaNoGoogleMaps(context, contacto.latitude!!, contacto.longitude!!)
                        true
                    }

                    mapView.overlays.add(marker)

                    if (isMaisProximo) markerMaisProximoObj = marker
                }
            }

            // 3. Foca a câmera no mais próximo
            if (markerMaisProximoObj != null) {
                mapView.controller.setCenter(markerMaisProximoObj!!.position)
                markerMaisProximoObj!!.showInfoWindow()
            } else if (localizacaoAtual != null) {
                mapView.controller.setCenter(GeoPoint(localizacaoAtual!!.latitude, localizacaoAtual!!.longitude))
            }

            mapView.invalidate()
        }
    )
}