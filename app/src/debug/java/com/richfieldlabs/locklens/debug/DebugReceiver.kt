package com.richfieldlabs.locklens.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Debug-only broadcast receiver for seeding screenshot data via ADB:
 *   adb shell am broadcast -a com.richfieldlabs.locklens.DEBUG_SEED
 */
@AndroidEntryPoint
class DebugReceiver : BroadcastReceiver() {

    @Inject
    lateinit var seedManager: DebugSeedManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SEED) return
        Log.d("DebugReceiver", "Seeding screenshot data...")
        val pending = goAsync()
        scope.launch {
            try {
                seedManager.seed()
                Log.d("DebugReceiver", "Seed complete. PIN is 123456.")
            } catch (e: Exception) {
                Log.e("DebugReceiver", "Seed failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SEED = "com.richfieldlabs.locklens.DEBUG_SEED"
    }
}
