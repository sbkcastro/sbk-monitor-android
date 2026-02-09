package com.sbkcastro.monitor.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sbkcastro.monitor.MainActivity
import com.sbkcastro.monitor.R
import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.ChatSendRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MinerScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val apiService = ApiClient.getService()

            // Enviar mensaje para detectar mineros
            val response = apiService.sendChat(
                ChatSendRequest(
                    message = "Detecta mineros de criptomonedas",
                    backend = "openclaw"
                )
            )

            // Si detecta alertas, mostrar notificación
            if (response.response.contains("ALERTA") || response.response.contains("🚨")) {
                showNotification(
                    title = "🚨 Minero Detectado",
                    message = "Se detectó actividad sospechosa en el servidor",
                    bigText = response.response
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(title: String, message: String, bigText: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificaciones
        val channelId = "miner_detection"
        val channel = NotificationChannel(
            channelId,
            "Detección de Mineros",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertas de mineros de criptomonedas"
        }
        notificationManager.createNotificationChannel(channel)

        // Intent para abrir la app
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Construir notificación
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_security)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 100
    }
}
