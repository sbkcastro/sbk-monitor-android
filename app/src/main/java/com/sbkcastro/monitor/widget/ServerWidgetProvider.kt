package com.sbkcastro.monitor.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.*
import com.sbkcastro.monitor.LoginActivity
import com.sbkcastro.monitor.R
import java.util.concurrent.TimeUnit

class ServerWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.sbkcastro.monitor.REFRESH_WIDGET") {
            // Refresh manual - ejecutar worker inmediatamente
            val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleUpdates(context)
    }

    override fun onEnabled(context: Context) {
        scheduleUpdates(context)
    }

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("sbk_widget_update")
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_server)

        // Leer desde charts_data en lugar de widget_data
        val prefs = context.getSharedPreferences("charts_data", Context.MODE_PRIVATE)
        val metricsJson = prefs.getString("metrics_data", null)

        if (metricsJson != null) {
            try {
                val json = org.json.JSONObject(metricsJson)
                val metricsArray = json.getJSONArray("metrics")

                if (metricsArray.length() > 0) {
                    // Obtener último punto de datos
                    val lastMetric = metricsArray.getJSONObject(metricsArray.length() - 1)
                    val cpu = String.format("%.1f", lastMetric.getDouble("cpu"))
                    val ram = String.format("%.1f", lastMetric.getDouble("ram"))
                    val disk = String.format("%.1f", lastMetric.getDouble("disk"))

                    views.setTextViewText(R.id.widgetCpu, "⚡ CPU: $cpu%")
                    views.setTextViewText(R.id.widgetRam, "🧠 RAM: $ram%")
                    views.setTextViewText(R.id.widgetDisk, "💾 Disco: $disk%")
                    views.setTextViewText(R.id.widgetStatus, "✅ Actualizado")
                } else {
                    setDefaultValues(views)
                }
            } catch (e: Exception) {
                setDefaultValues(views)
            }
        } else {
            setDefaultValues(views)
        }

        // Click en el widget abre la app
        val intent = Intent(context, LoginActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

        // Click en refresh actualiza manualmente
        val refreshIntent = Intent(context, ServerWidgetProvider::class.java).apply {
            action = "com.sbkcastro.monitor.REFRESH_WIDGET"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetRefresh, refreshPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setDefaultValues(views: RemoteViews) {
        views.setTextViewText(R.id.widgetCpu, "⚡ CPU: --%")
        views.setTextViewText(R.id.widgetRam, "🧠 RAM: --%")
        views.setTextViewText(R.id.widgetDisk, "💾 Disco: --%")
        views.setTextViewText(R.id.widgetStatus, "Iniciando...")
    }

    private fun scheduleUpdates(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(10, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sbk_widget_update", ExistingPeriodicWorkPolicy.KEEP, request
        )
    }
}
