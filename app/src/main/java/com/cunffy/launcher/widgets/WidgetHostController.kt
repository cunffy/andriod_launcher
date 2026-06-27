package com.cunffy.launcher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle

/**
 * Wraps [AppWidgetHost] so the launcher can host third-party home-screen widgets.
 *
 * Lifecycle: call [startListening] when the home screen is resumed and [stopListening] when
 * paused. Adding a widget uses the system picker (see WidgetPicker) which allocates an id and
 * binds the provider; we then build a hosted view with [createView].
 */
class WidgetHostController(context: Context) {

    private val appContext = context.applicationContext
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    private val host = AppWidgetHost(appContext, HOST_ID)

    fun startListening() = runCatching { host.startListening() }
    fun stopListening() = runCatching { host.stopListening() }

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun deleteId(widgetId: Int) = runCatching { host.deleteAppWidgetId(widgetId) }

    /** All widget providers installed on the device (for the custom widget picker). */
    fun installedProviders(): List<AppWidgetProviderInfo> =
        runCatching { appWidgetManager.installedProviders }.getOrDefault(emptyList())

    /** Binds [widgetId] to [provider] if the launcher already holds bind permission. */
    fun bindIfAllowed(widgetId: Int, provider: android.content.ComponentName): Boolean =
        runCatching { appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, provider) }
            .getOrDefault(false)

    fun providerInfo(widgetId: Int): AppWidgetProviderInfo? =
        appWidgetManager.getAppWidgetInfo(widgetId)

    fun createView(context: Context, widgetId: Int): AppWidgetHostView? {
        val info = providerInfo(widgetId) ?: return null
        return runCatching { host.createView(context, widgetId, info) }.getOrNull()
    }

    /**
     * Tell the widget its on-screen size (in dp) so it can pick the right responsive layout.
     * Without this many widgets render at a default/min size or look clipped.
     */
    fun updateSize(widgetId: Int, widthDp: Int, heightDp: Int) {
        if (widthDp <= 0 || heightDp <= 0) return
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
        }
        runCatching { appWidgetManager.updateAppWidgetOptions(widgetId, options) }
    }

    companion object {
        const val HOST_ID = 0x4C41 // "LA"
    }
}
