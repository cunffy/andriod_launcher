package com.cunffy.launcher.gesture

/** Actions that can be bound to launcher gestures in the gesture editor. */
enum class GestureAction(val label: String) {
    NONE("Nothing"),
    OPEN_DRAWER("Open app drawer"),
    OPEN_SEARCH("Open search"),
    EXPAND_NOTIFICATIONS("Notification shade"),
    EXPAND_QUICK_SETTINGS("Quick settings"),
    LOCK_SCREEN("Lock screen"),
    OPEN_LAUNCHER_SETTINGS("Launcher settings"),
    ;

    companion object {
        fun fromName(name: String?): GestureAction =
            entries.firstOrNull { it.name == name } ?: NONE
    }
}

/** The gestures a user can bind. */
enum class GestureSlot(val defaultAction: GestureAction) {
    SWIPE_UP(GestureAction.OPEN_DRAWER),
    SWIPE_DOWN(GestureAction.EXPAND_NOTIFICATIONS),
    DOUBLE_TAP(GestureAction.NONE),
    PINCH_IN(GestureAction.OPEN_LAUNCHER_SETTINGS),
    PINCH_OUT(GestureAction.NONE),
}
