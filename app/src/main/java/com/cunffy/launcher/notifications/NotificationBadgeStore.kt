package com.cunffy.launcher.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory map of package name → active notification count, fed by the listener service. */
@Singleton
class NotificationBadgeStore @Inject constructor() {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    fun update(counts: Map<String, Int>) {
        _counts.value = counts
    }
}
