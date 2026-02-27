package com.sbkcastro.monitor

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sistema de telemetría de crashes custom
 * Captura excepciones no manejadas y las envía al backend
 */
class CrashReporter private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("crash_reports", Context.MODE_PRIVATE)
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        private const val TAG = "CrashReporter"
        private const val MAX_CRASHES_STORED = 10
        private const val PREF_CRASH_COUNT = "crash_count"
        private const val PREF_CRASH_PREFIX = "crash_"

        @Volatile
        private var instance: CrashReporter? = null

        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = CrashReporter(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(): CrashReporter? = instance
    }

    init {
        setupExceptionHandler()
        sendStoredCrashes()
    }

    private fun setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                handleCrash(thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling crash", e)
            }

            // Llamar al handler original
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun handleCrash(thread: Thread, throwable: Throwable) {
        val crashReport = buildCrashReport(thread, throwable)
        storeCrash(crashReport)
        Log.e(TAG, "💥 CRASH CAPTURED:\n$crashReport")
    }

    private fun buildCrashReport(thread: Thread, throwable: Throwable): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val stackTrace = getStackTraceString(throwable)

        return """
=== CRASH REPORT ===
Time: $timestamp
App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
Device: ${Build.MANUFACTURER} ${Build.MODEL}
Thread: ${thread.name}

Exception: ${throwable.javaClass.simpleName}
Message: ${throwable.message ?: "No message"}

Stack Trace:
$stackTrace

==================
        """.trimIndent()
    }

    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }

    private fun storeCrash(crashReport: String) {
        val count = prefs.getInt(PREF_CRASH_COUNT, 0)
        val newCount = (count + 1).coerceAtMost(MAX_CRASHES_STORED)

        prefs.edit().apply {
            putInt(PREF_CRASH_COUNT, newCount)
            putString("$PREF_CRASH_PREFIX$newCount", crashReport)
            apply()
        }
    }

    private fun sendStoredCrashes() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val count = prefs.getInt(PREF_CRASH_COUNT, 0)
                if (count == 0) return@launch

                val crashes = mutableListOf<String>()
                for (i in 1..count) {
                    prefs.getString("$PREF_CRASH_PREFIX$i", null)?.let { crashes.add(it) }
                }

                if (crashes.isNotEmpty()) {
                    sendCrashesToBackend(crashes)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending stored crashes", e)
            }
        }
    }

    private suspend fun sendCrashesToBackend(crashes: List<String>) {
        try {
            // Intentar enviar crashes al backend
            val service = com.sbkcastro.monitor.api.ApiClient.getService()
            val crashData = mapOf(
                "crashes" to crashes,
                "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "version" to BuildConfig.VERSION_NAME
            )

            // Endpoint que crearemos en backend
            // service.reportCrashes(crashData)

            // Por ahora, solo log
            Log.i(TAG, "📤 Would send ${crashes.size} crashes to backend")
            crashes.forEach { crash ->
                Log.d(TAG, "Crash:\n$crash")
            }

            // Limpiar crashes enviados
            clearStoredCrashes()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send crashes to backend", e)
        }
    }

    private fun clearStoredCrashes() {
        prefs.edit().clear().apply()
    }

    /**
     * Reportar excepción manualmente (no fatal)
     */
    fun logException(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, "⚠️ NON-FATAL: $message", throwable)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val report = """
=== NON-FATAL EXCEPTION ===
Tag: $tag
Message: $message
Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}

${getStackTraceString(throwable)}
==========================
                """.trimIndent()

                Log.d(TAG, report)
                // Aquí se podría enviar al backend también
            } catch (e: Exception) {
                Log.e(TAG, "Error logging exception", e)
            }
        }
    }

    /**
     * Log evento importante para debugging
     */
    fun logEvent(tag: String, event: String, data: Map<String, Any> = emptyMap()) {
        val dataStr = data.entries.joinToString(", ") { "${it.key}=${it.value}" }
        Log.i(tag, "📊 EVENT: $event ${if (dataStr.isNotEmpty()) "[$dataStr]" else ""}")
    }
}
