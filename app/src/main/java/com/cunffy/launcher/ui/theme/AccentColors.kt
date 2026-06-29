package com.cunffy.launcher.ui.theme

import com.cunffy.launcher.data.prefs.AccentPreset

/** The ARGB seed color for each accent preset, shared by the theme and themed-icon tinting. */
fun AccentPreset.argb(): Int = when (this) {
    AccentPreset.GREEN -> 0xFF4CAF50.toInt()
    AccentPreset.BLUE -> 0xFF2196F3.toInt()
    AccentPreset.PURPLE -> 0xFF9C27B0.toInt()
    AccentPreset.ORANGE -> 0xFFFF9800.toInt()
    AccentPreset.RED -> 0xFFF44336.toInt()
    AccentPreset.TEAL -> 0xFF009688.toInt()
}
