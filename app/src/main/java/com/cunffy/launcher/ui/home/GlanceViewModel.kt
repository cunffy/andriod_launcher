package com.cunffy.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cunffy.launcher.data.glance.CalendarRepository
import com.cunffy.launcher.data.glance.GlanceEvent
import com.cunffy.launcher.data.glance.Weather
import com.cunffy.launcher.data.glance.WeatherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GlanceViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val weatherProvider: WeatherProvider,
) : ViewModel() {

    data class GlanceState(val event: GlanceEvent? = null, val weather: Weather? = null)

    private val _state = MutableStateFlow(GlanceState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = GlanceState(
                event = calendarRepository.nextEvent(),
                weather = weatherProvider.current(),
            )
        }
    }
}
