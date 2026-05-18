package com.example.myapplication.utils

import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import java.io.IOException

object MapUtils {
    
    // Abre a rota direcionando para um app de navegação externo
    fun abrirRotaNoGoogleMaps(context: Context, lat: Double, lng: Double) {
        val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        }
    }

    // Calcula a distância entre dois pontos geográficos e retorna em metros
    fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    // Converte o endereço de texto (String) em coordenadas de GPS válidas (Geocoding)
    fun obterCoordenadasPorEndereco(context: Context, endereco: String): Address? {
        val geocoder = Geocoder(context)
        try {
            // O número 1 indica que queremos apenas o primeiro resultado mais preciso
            @Suppress("DEPRECATION")
            val resultados = geocoder.getFromLocationName(endereco, 1)
            if (!resultados.isNullOrEmpty()) {
                return resultados[0]
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}