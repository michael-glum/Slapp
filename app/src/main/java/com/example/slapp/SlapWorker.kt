package com.example.slapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Timer
import java.util.TimerTask

class SlapWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val appsToMonitor = inputData.getStringArray("appsToMonitor")?.toList<String>()
        val fad = ForegroundAppDetector()
        val timeUntilSlap: Long = 1000 * 60 * 5

        // Do the work here--in this case, prepare the slap.
        if (appsToMonitor != null) {
            prepareSlap(appsToMonitor, fad, timeUntilSlap)
        }

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }

    private fun prepareSlap(appsToMonitor: List<String>, fad: ForegroundAppDetector,
                            timeUntilSlap: Long) {
        if (isWastingTime(appsToMonitor, fad, timeUntilSlap)) {
            sendSlapNotification()
        }
    }

    private fun isWastingTime(appsToMonitor: List<String>, fad: ForegroundAppDetector,
                              timeUntilSlap: Long): Boolean {
        val foregroundApp = fad.getForegroundApp(applicationContext, appsToMonitor)
        var isWastingTime = false

        if (foregroundApp in appsToMonitor) {
//            object: CountDownTimer(timeUntilSlap, 5000){
//                override fun onTick(p0: Long) {
//                    if (foregroundApp != fad.getForegroundApp(applicationContext, appsToMonitor)) {
//                        return
//                    }
//                }
//                override fun onFinish() {
//                    isWastingTime = true
//                }
//            }.start()
            isWastingTime = true
        }
        return isWastingTime
    }

    private fun sendSlapNotification() {
        val notificationID = 0 // this probably doesn't belong here
        val title = applicationContext.getString(R.string.slap_notification_title)
        val text = applicationContext.getString(R.string.slap_notification_text)

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.openhandbutton)

        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            notify(notificationID, builder.build())
        }
    }


    /* This doesn't belong here. Alternative: android.permission.QUERY_ALL_PACKAGES
    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    } */
}
