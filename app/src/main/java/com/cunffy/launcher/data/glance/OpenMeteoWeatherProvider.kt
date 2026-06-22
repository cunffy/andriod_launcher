package com.cunffy.launcher.data.glance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.cunffy.launcher.core.Http
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Free, key-less weather via the Open-Meteo API. Uses the last known coarse location; returns
 * null (so At-a-Glance simply hides) when location permission or a fix isn't available.
 */
class OpenMeteoWeatherProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : WeatherProvider {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Response(val current: Current? = null)

    @Serializable
    private data class Current(
        val temperature_2m: Double? = null,
        val weather_code: Int? = null,
    )

    override suspend fun current(): Weather? {
        val location = lastKnownLocation() ?: return null
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${location.first}&longitude=${location.second}" +
            "&current=temperature_2m,weather_code"
        val body = Http.getString(url) ?: return null
        val current = runCatching { json.decodeFromString(Response.serializer(), body) }
            .getOrNull()?.current ?: return null
        val temp = current.temperature_2m ?: return null
        return Weather(
            temperatureC = temp.roundToInt(),
            description = describe(current.weather_code),
        )
    }

    private fun lastKnownLocation(): Pair<Double, Double>? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER,
        )
        for (provider in providers) {
            val loc = runCatching {
                if (lm.isProviderEnabled(provider)) lm.getLastKnownLocation(provider) else null
            }.getOrNull()
            if (loc != null) return loc.latitude to loc.longitude
        }
        return null
    }

    private fun describe(code: Int?): String = when (code) {
        0 -> "Clear"
        1, 2, 3 -> "Partly cloudy"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        in 85..86 -> "Snow showers"
        in 95..99 -> "Thunderstorm"
        else -> "Weather"
    }
}
