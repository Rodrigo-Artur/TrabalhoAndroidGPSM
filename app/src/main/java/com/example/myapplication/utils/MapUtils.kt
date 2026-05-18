package com.example.myapplication.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.location.Location

object MapUtils {
    // Abre a rota no Google Maps
    fun abrirRotaNoGoogleMaps(context: Context, lat: Double, lng: Double) {
        val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        }
    }

    // Calcula a distância entre dois pontos e retorna a distância em metros
    fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
}