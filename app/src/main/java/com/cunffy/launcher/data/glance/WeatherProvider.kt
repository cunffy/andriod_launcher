package com.cunffy.launcher.data.glance

import javax.inject.Inject

data class Weather(val temperatureC: Int, val description: String)

/**
 * Pluggable weather source for At-a-Glance. The default implementation returns null because
 * live weather needs a network API key the launcher doesn't ship with; swap in a real
 * implementation (e.g. backed by a weather API) via the DI binding in AppModule.
 */
interface WeatherProvider {
    suspend fun current(): Weather?
}

class NoopWeatherProvider @Inject constructor() : WeatherProvider {
    override suspend fun current(): Weather? = null
}
