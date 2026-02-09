package com.sbkcastro.monitor.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.LoginRequest

class WidgetUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val securePrefs = context.getSharedPreferences("sbk_secure_prefs", Context.MODE_PRIVATE)
            val serverUrl = securePrefs.getString("server_url", "") ?: return Result.failure()
            val token = securePrefs.getString("auth_token", "") ?: return Result.failure()

            if (serverUrl.isEmpty() || token.isEmpty()) return Result.failure()

            ApiClient.initialize(serverUrl)
            ApiClient.setToken(token)

            val metrics = ApiClient.getService().getMetrics()

            val widgetPrefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
            widgetPrefs.edit()
                .putString("cpu", metrics.cpu.usage.toString())
                .putString("ram", metrics.memory.usagePercent.toString())
                .putString("disk", metrics.disk.usagePercent.toString())
                .putString("status", "Online")
                .apply()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ServerWidgetProvider::class.java)
            )
            if (widgetIds.isNotEmpty()) {
                val intent = Intent(context, ServerWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                context.sendBroadcast(intent)
            }

            return Result.success()
        } catch (e: Exception) {
            val widgetPrefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
            widgetPrefs.edit().putString("status", "Offline").apply()
            return Result.retry()
        }
    }
}
