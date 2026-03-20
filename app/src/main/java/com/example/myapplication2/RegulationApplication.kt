package com.example.myapplication2

import android.app.Application
import android.content.res.Configuration
import android.util.Log
import com.example.myapplication2.di.AppContainer
import com.example.myapplication2.notifications.RegulatoryNotificationService
import java.util.Locale

class RegulationApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
    val container: AppContainer get() = appContainer

    override fun onCreate() {
        super.onCreate()
        RegulatoryNotificationService.initialize(this)
        Locale.setDefault(Locale.ENGLISH)
        val config = Configuration(resources.configuration)
        config.setLocale(Locale.ENGLISH)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MyApp2", "CRASH: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
