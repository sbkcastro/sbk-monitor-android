package com.sbkcastro.monitor.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sbkcastro.monitor.MainActivity
import com.sbkcastro.monitor.R
import com.sbkcastro.monitor.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ClaudeApprovalWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "claude_approval"
        const val WORK_NAME = "claude_approval_poll"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ClaudeApprovalWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Claude Tool Approval",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Aprobación de comandos Claude Code"
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val resp = ApiClient.getService().claudeJobPending()
            for (item in resp.pending) {
                showApprovalNotification(
                    jobId = item.jobId,
                    tool = item.tool,
                    command = item.command,
                    sessionName = item.firstMessage
                )
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun showApprovalNotification(jobId: String, tool: String, command: String, sessionName: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val approveIntent = PendingIntent.getBroadcast(
            applicationContext,
            jobId.hashCode(),
            Intent(applicationContext, ApprovalActionReceiver::class.java).apply {
                action = "CLAUDE_APPROVE"
                putExtra("jobId", jobId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val denyIntent = PendingIntent.getBroadcast(
            applicationContext,
            jobId.hashCode() + 1,
            Intent(applicationContext, ApprovalActionReceiver::class.java).apply {
                action = "CLAUDE_DENY"
                putExtra("jobId", jobId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val tapIntent = PendingIntent.getActivity(
            applicationContext,
            jobId.hashCode() + 2,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_security)
            .setContentTitle("🤖 Claude necesita permiso")
            .setContentText("$tool: ${command.take(80)}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Herramienta: $tool\nComando: $command\nSesión: \"$sessionName\"")
            )
            .addAction(0, "✅ Aprobar", approveIntent)
            .addAction(0, "❌ Denegar", denyIntent)
            .setContentIntent(tapIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(jobId.hashCode(), notification)
    }
}

class ApprovalActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val jobId = intent.getStringExtra("jobId") ?: return
        val action = intent.action ?: return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(jobId.hashCode())

        val token = ApiClient.getToken() ?: return
        val endpoint = if (action == "CLAUDE_APPROVE") "approve" else "deny"
        val baseUrl = ApiClient.getBaseUrl()

        Thread {
            try {
                val url = java.net.URL("${baseUrl}api/claude/job/$jobId/$endpoint")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.connect()
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) { }
        }.start()
    }
}
