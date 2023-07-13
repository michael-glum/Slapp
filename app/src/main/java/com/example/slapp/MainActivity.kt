package com.example.slapp

import android.app.NotificationManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.preference.PreferenceManager
import androidx.work.Data
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.slapp.ui.theme.SlappTheme
import java.time.Duration

const val CHANNEL_ID = "1"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SlappTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }

        // Create notification channel to send slaps.
        setUpSlapNotificationChannel()

        // Determine which apps should be monitored
        val appsToMonitor = getAppsToMonitor()

        // Create worker to monitor restricted apps and send slap notifications, even after the
        // app is closed
        val workRepeatInterval : Duration = Duration.ofMinutes(5)
        createBackgroundWorker(appsToMonitor, workRepeatInterval)
    }

    private fun getAppsToMonitor():
            ArrayList<String> {
        val installedApps = getInstalledApplications()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val appsToMonitor = arrayListOf<String>()
        for (i in installedApps) {
            if (sharedPreferences.getBoolean(i, false)) {
                appsToMonitor.add(i)
            }
        }
        return appsToMonitor
    }

    private fun getInstalledApplications(): ArrayList<String> {
        val packageManager = this.packageManager
        val applicationList = packageManager.getInstalledApplications(
            PackageManager.ApplicationInfoFlags.of(0))
        val installedApps = ArrayList<String>()
        for (i in applicationList.indices) {
            val applicationInfo = applicationList[i]
            val validCategories = arrayOf(ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_GAME, ApplicationInfo.CATEGORY_SOCIAL)
            if (applicationInfo.category in validCategories) {
                installedApps.add(applicationInfo.packageName)
            }
        }
        return installedApps
    }

    private fun setUpSlapNotificationChannel() {
        val notificationHandler = NotificationHandler()
        notificationHandler.createChannel(this, getString(R.string.slap_channel_name), getString(R.string.slap_channel_description),
            NotificationManager.IMPORTANCE_HIGH)
    }

    private fun createBackgroundWorker(appsToMonitor: ArrayList<String>,
                                       workRepeatInterval: Duration) {
        val data = Data.Builder()
        data.putStringArray("appsToMonitor", appsToMonitor.toTypedArray())

        val notificationWorkRequest: PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<SlapWorker>(workRepeatInterval)
                .setInputData(data.build()).build()

        WorkManager
            .getInstance(applicationContext)
            .enqueue(notificationWorkRequest)
    }
}

@Composable
fun SharedPrefsToggle(text: String, value: Boolean, onValueChanged: (Boolean) -> Unit) {
    Row {
        Text(text)
        Checkbox(checked = value, onCheckedChange = onValueChanged)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SlappTheme {
        Greeting("Android")
    }
}