package com.cunffy.launcher.data.glance

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import com.cunffy.launcher.core.Http
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.coroutines.resume
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
        val location = resolveLocation() ?: return null
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

    @SuppressLint("MissingPermission")
    private suspend fun resolveLocation(): Pair<Double, Double>? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        // Prefer a recent last-known fix; fall back to requesting a fresh one (which is what
        // makes weather appear on devices where nothing else has asked for location yet).
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

        val freshProvider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }
        val fresh: Location? = withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine { cont ->
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                runCatching {
                    lm.getCurrentLocation(freshProvider, signal, context.mainExecutor) { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                }.onFailure { if (cont.isActive) cont.resume(null) }
            }
        }
        return fresh?.let { it.latitude to it.longitude }
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
