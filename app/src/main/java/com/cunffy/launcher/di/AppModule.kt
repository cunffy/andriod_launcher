package com.cunffy.launcher.di

import com.cunffy.launcher.data.glance.NoopWeatherProvider
import com.cunffy.launcher.data.glance.WeatherProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    /** Swap [NoopWeatherProvider] for a network-backed implementation to enable weather. */
    @Binds
    @Singleton
    abstract fun bindWeatherProvider(impl: NoopWeatherProvider): WeatherProvider
}
