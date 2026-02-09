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

        val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
        views.setTextViewText(R.id.widgetCpu, "CPU: ${prefs.getString("cpu", "--")}%")
        views.setTextViewText(R.id.widgetRam, "RAM: ${prefs.getString("ram", "--")}%")
        views.setTextViewText(R.id.widgetDisk, "Disk: ${prefs.getString("disk", "--")}%")
        views.setTextViewText(R.id.widgetStatus, prefs.getString("status", "SBK Monitor"))

        val intent = Intent(context, LoginActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun scheduleUpdates(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sbk_widget_update", ExistingPeriodicWorkPolicy.KEEP, request
        )
    }
}
