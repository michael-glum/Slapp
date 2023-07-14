package com.example.slapp

import android.app.NotificationManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.slapp.ui.theme.SlapBlue
import com.example.slapp.ui.theme.SlapBlue2
import com.example.slapp.ui.theme.SlappTheme
import com.example.slapp.ui.theme.alfaSlabOneFont
import java.time.Duration

const val CHANNEL_ID = "1"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get relevant installed applications
        val appList = getInstalledApplications()

        setContent {
            SlappTheme {
                // A surface container using the 'background' color from the theme
                MyApp(modifier = Modifier.fillMaxSize())
            }
        }

        // Create notification channel to send slaps.
        setUpSlapNotificationChannel()

        // Determine which apps should be monitored
        val appsToMonitor = getAppsToMonitor(appList)

        // Create worker to monitor restricted apps and send slap notifications, even after the
        // app is closed
        val workRepeatInterval : Duration = Duration.ofMinutes(15)
        val workFlexInterval : Duration = Duration.ofMinutes(14)
        createBackgroundWorker(appsToMonitor, workRepeatInterval, workFlexInterval)
    }

    private fun getAppsToMonitor(appList: ArrayList<String>):
            ArrayList<String> {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val appsToMonitor = arrayListOf<String>()
        for (i in appList) {
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
                                       workRepeatInterval: Duration, workFlexInterval: Duration) {
        val data = Data.Builder()
        data.putStringArray("appsToMonitor", appsToMonitor.toTypedArray())

        val notificationWorkRequest: PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<SlapWorker>(workRepeatInterval, workFlexInterval)
                .setInputData(data.build()).build()

        WorkManager
            .getInstance(applicationContext).enqueueUniquePeriodicWork("SlapWorker",
                ExistingPeriodicWorkPolicy.UPDATE, notificationWorkRequest)
    }
}

@Composable
private fun MyApp(modifier: Modifier = Modifier) {

    val shouldShowSettings = remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        if (shouldShowSettings.value) {
            SettingsScreen(onCloseClicked = { shouldShowSettings.value = false })
        } else {
            Greeting(onSettingsClicked = { shouldShowSettings.value = true })
        }
    }
}

@Composable
fun SettingsScreen(onCloseClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SlapBlue, SlapBlue2)
                )
            )
    ){
        MyIconButton(onCloseClicked, 633)
    }
}

@Composable
fun MyIconButton(onButtonClicked: () -> Unit, topPadding: Int) {
    IconButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding.dp),
        onClick = onButtonClicked
    ) {
        Icon(
            painter = painterResource(R.drawable.settingsbutton),
            contentDescription = null,
            modifier = Modifier.size(size = 45.dp),
            tint = Color.White
        )
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
fun Greeting(onSettingsClicked: () -> Unit) {
    val isActive = remember { mutableStateOf(false) }
    val status = if (isActive.value) "Deactivate" else "Activate"
    val myImage = if (isActive.value) R.drawable.openhandbutton else R.drawable.closedfistbutton

    Column(
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SlapBlue, SlapBlue2)
                )
            )
            .fillMaxSize()
    ) {
        Text (
            text = "Slapp!",
            textAlign = TextAlign.Center,
            fontSize = 75.sp,
            fontFamily = alfaSlabOneFont,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp, bottom = 75.dp),
            color = Color.White,

        )
        Text (
            text = "Tap to $status",
            textAlign = TextAlign.Center,
            fontSize = 25.sp,
            fontFamily = alfaSlabOneFont,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 50.dp),
            color = Color.White
        )
        Image(
            painter = painterResource(myImage),
            contentDescription = null,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(bottom = 100.dp)
                .clickable{ isActive.value = !isActive.value }
        )
        MyIconButton(onSettingsClicked, 0)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SlappTheme {
        MyApp()
    }
}