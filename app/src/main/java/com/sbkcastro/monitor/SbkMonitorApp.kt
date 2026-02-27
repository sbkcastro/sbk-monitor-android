package com.sbkcastro.monitor

import android.app.Application
import android.util.Log

class SbkMonitorApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicializar crash reporter
        CrashReporter.initialize(this)
        Log.i("SbkMonitorApp", "✅ CrashReporter initialized - Telemetry active")

        // Log evento de inicio
        CrashReporter.getInstance()?.logEvent(
            "AppLifecycle",
            "App Started",
            mapOf(
                "version" to BuildConfig.VERSION_NAME,
                "versionCode" to BuildConfig.VERSION_CODE
            )
        )
    }
}
