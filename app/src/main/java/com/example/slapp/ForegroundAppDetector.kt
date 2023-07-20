package com.example.slapp

import android.Manifest
import android.app.AppOpsManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService

class ForegroundAppDetector {
    fun getForegroundApp(context: Context, appsToMonitor: List<String>): String? {
        var foregroundApp: String? = null
        val mUsageStatsManager =
            context.getSystemService(Service.USAGE_STATS_SERVICE) as UsageStatsManager
//        if (!checkUsageStatsPermission()) {
//            return "nope"
//        }
        val time = System.currentTimeMillis()
        val usageEvents = mUsageStatsManager.queryEvents(time - 1000 * 60 * 60 * 14, time)
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (event.packageName in appsToMonitor) {
                    foregroundApp = event.packageName
                }
            }
        }
        return foregroundApp
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