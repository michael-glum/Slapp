package com.example.slapp

import android.app.AppOpsManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat.getSystemService
import java.util.SortedMap
import java.util.TreeMap


class ForegroundAppDetector {
    fun getForegroundApp(context: Context, appsToMonitor: List<String>): String? {
        requestUsageStatsPermission(context)
        var foregroundApp: String? = null
        val usageStatsManager =
            context.getSystemService(Service.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()

        // Get foreground app if there is a recent interaction
        val interactionTimeDifference = 1000 * 30
        var usageEvents = usageStatsManager.queryEvents(currentTime - interactionTimeDifference, currentTime)
        var event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.USER_INTERACTION/*UsageEvents.Event.ACTIVITY_RESUMED*/) {
                if (event.packageName in appsToMonitor) {
                    foregroundApp = event.packageName

                }
            }
            if (foregroundApp != null &&
                (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                        event.eventType == UsageEvents.Event.ACTIVITY_STOPPED)) {
                if (event.packageName == foregroundApp) {
                    foregroundApp = null
                }
            }
        }

        // Get foreground app if there isn't a recent interaction
        if (foregroundApp == null) {
            val resumedTimeDifference = 1000 * 60 * 60 * 24
            usageEvents = usageStatsManager.queryEvents(currentTime - resumedTimeDifference,
                currentTime)
            event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    if (event.packageName in appsToMonitor) {
                        foregroundApp = event.packageName
                    }
                }
//                if (foregroundApp != null &&
//                    (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
//                            event.eventType == UsageEvents.Event.ACTIVITY_STOPPED)) {
//                    if (event.packageName == foregroundApp) {
//                        foregroundApp = null
//                    }
//                }
            }
        } else {
            Log.e("Interaction detected", "Interaction was detected")
        }
        Log.e("Foreground App", "The foreground app is: $foregroundApp")
        return foregroundApp
    }

    private fun requestUsageStatsPermission(context: Context) {
        if (!hasUsageStatsPermission(context)
        ) {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps =
            context.getSystemService(ComponentActivity.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            "android:get_usage_stats",
            Process.myUid(), context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

//    private fun checkUsageStatsPermission() : Boolean {
//        val appOpsManager = AppCompatActivity.APP_OPS_SERVICE as AppOpsManager
//        // `AppOpsManager.checkOpNoThrow` is deprecated from Android Q
//        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            appOpsManager.unsafeCheckOpNoThrow(
//                "android:get_usage_stats",
//                android.os.Process.myUid(), "com.google.android.youtube"
//            )
//        }
//        else {
//            appOpsManager.checkOpNoThrow(
//                "android:get_usage_stats",
//                android.os.Process.myUid(), "com.google.android.youtube"
//            )
//        }
//        return mode == AppOpsManager.MODE_ALLOWED
//    }

}