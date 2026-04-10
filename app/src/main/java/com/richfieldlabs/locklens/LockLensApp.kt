package com.richfieldlabs.locklens

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LockLensApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            INTRUDER_CHANNEL_ID,
            "Intrusion alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alerts when someone fails to unlock your vault"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val INTRUDER_CHANNEL_ID = "intruder_alerts"
    }
}

