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

            if (serverUrl.isEmpty()) return Result.failure()

            ApiClient.initialize(context, serverUrl)

            // El token se obtiene automáticamente desde AuthInterceptor
            val metrics = ApiClient.getService().getMetrics()

            // Usar charts_data para consistencia con el ViewModel
            val chartsPrefs = context.getSharedPreferences("charts_data", Context.MODE_PRIVATE)
            val metricsJson = chartsPrefs.getString("metrics_data", null)
            val json = if (metricsJson != null) org.json.JSONObject(metricsJson) else org.json.JSONObject()

            // Agregar nuevo punto de datos
            val metricsArray = json.optJSONArray("metrics") ?: org.json.JSONArray()
            val newMetric = org.json.JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("cpu", metrics.cpu.usage.toDouble())
                put("ram", metrics.memory.usagePercent.toDouble())
                put("disk", metrics.disk.usagePercent.toDouble())
            }
            metricsArray.put(newMetric)
            json.put("metrics", metricsArray)

            chartsPrefs.edit().putString("metrics_data", json.toString()).apply()

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
