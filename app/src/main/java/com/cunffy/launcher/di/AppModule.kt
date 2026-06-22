package com.cunffy.launcher.di

import com.cunffy.launcher.data.glance.OpenMeteoWeatherProvider
import com.cunffy.launcher.data.glance.WeatherProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    /** Open-Meteo (key-less) weather; falls back to no data without location/network. */
    @Binds
    @Singleton
    abstract fun bindWeatherProvider(impl: OpenMeteoWeatherProvider): WeatherProvider
}
