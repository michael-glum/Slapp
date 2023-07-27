package com.example.slapp

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.lang.reflect.Field
import kotlin.random.Random


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
            isWastingTime = true
        }
        return isWastingTime
    }

    private fun sendSlapNotification() {
        val notificationID = 0
        val title = applicationContext.getString(R.string.slap_notification_title)

        var resName = "slap_notification_text_19"
        val start = 0
        val endInclusive = 25
        //resName += (start..endInclusive).random()

        val text = applicationContext.getString(getResId(resName, R.string::class.java))

        // Create intent to open slapp, allowing user to be returned to the home
        // screen of the android device
        val closeIntent = Intent(applicationContext, MainActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("EXIT", true)
            startActivity(applicationContext,this, null)
        }
        // Create the TaskStackBuilder
        val closePendingIntent: PendingIntent? = TaskStackBuilder.create(applicationContext).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(closeIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        // Create action
        val closeAction: NotificationCompat.Action =
            NotificationCompat.Action.Builder(null,
                applicationContext.getText(R.string.slap_notification_action),
                closePendingIntent).build()


        // Build notification
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.openhandbutton)
            .setLargeIcon(
                BitmapFactory.decodeResource(applicationContext.resources,
                R.drawable.notification_large_icon))
            .setAutoCancel(false)
            .setColor(applicationContext.getColor(R.color.slap_blue))
            .setColorized(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .addAction(closeAction)

        // Send notification (double-check that it has the correct permissions)
        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(notificationID, builder.build())
        }
    }

    private fun getResId(resName: String, c: Class<*>): Int {
        return try {
            val idField: Field = c.getDeclaredField(resName)
            idField.getInt(idField)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    private fun IntRange.random() =
        Random.nextInt((endInclusive + 1) - start) + start
}
