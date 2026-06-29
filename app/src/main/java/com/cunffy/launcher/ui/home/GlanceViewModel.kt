package com.cunffy.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cunffy.launcher.data.glance.CalendarRepository
import com.cunffy.launcher.data.glance.GlanceEvent
import com.cunffy.launcher.data.glance.Weather
import com.cunffy.launcher.data.glance.WeatherProvider
import com.cunffy.launcher.data.prefs.LauncherPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GlanceViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val weatherProvider: WeatherProvider,
    private val preferences: LauncherPreferences,
) : ViewModel() {

    data class GlanceState(
        val event: GlanceEvent? = null,
        val weather: Weather? = null,
        val countdown: Boolean = false,
    )

    private val _state = MutableStateFlow(GlanceState())
    val state = _state.asStateFlow()

    /** Upcoming events for the long-press picker, loaded on demand. */
    private val _upcoming = MutableStateFlow<List<GlanceEvent>>(emptyList())
    val upcoming = _upcoming.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val settings = preferences.settings.first()
            // Show the pinned event when one is chosen and still upcoming; else the next event.
            val event = settings.glanceEventId
                .takeIf { it >= 0 }
                ?.let { calendarRepository.eventById(it) }
                ?: calendarRepository.nextEvent()
            _state.value = GlanceState(
                event = event,
                weather = weatherProvider.current(),
                countdown = settings.glanceCountdown,
            )
        }
    }

    fun loadUpcoming() {
        viewModelScope.launch { _upcoming.value = calendarRepository.upcomingEvents() }
    }

    fun pinEvent(id: Long) {
        viewModelScope.launch {
            preferences.setGlanceEventId(id)
            refresh()
        }
    }

    fun useNextEvent() {
        viewModelScope.launch {
            preferences.setGlanceEventId(-1L)
            refresh()
        }
    }

    fun setCountdown(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setGlanceCountdown(enabled)
            refresh()
        }
    }
}
