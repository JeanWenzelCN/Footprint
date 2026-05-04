package com.footprint.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.footprint.service.LocationTrackingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE)
            val shouldResumeTracking = prefs.getBoolean("is_tracking", false)
            val isPaused = prefs.getBoolean("is_paused", false)
            if (shouldResumeTracking && !isPaused) {
                Log.d("BootReceiver", "Boot completed, restoring active tracking service")
                LocationTrackingService.restoreTracking(context)
            } else {
                Log.d("BootReceiver", "Boot completed, no active tracking session to restore")
            }
        }
    }
}
