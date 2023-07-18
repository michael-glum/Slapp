package com.example.slapp

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import java.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.Console

const val CHANNEL_ID = "1"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel by viewModels<SettingsDataViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get relevant installed applications
        val appList = getInstalledApplications()

        setContent {
            SlappTheme {
                // A surface container using the 'background' color from the theme
                MyApp(modifier = Modifier.fillMaxSize(), settingsViewModel)
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
        notificationHandler.createChannel(this, getString(R.string.slap_channel_name),
            getString(R.string.slap_channel_description), NotificationManager.IMPORTANCE_HIGH)
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
                ExistingPeriodicWorkPolicy.KEEP, notificationWorkRequest)
    }
}

@Composable
private fun MyApp(modifier: Modifier = Modifier, viewModel: SettingsDataViewModel) {

    val shouldShowSettings = remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        if (shouldShowSettings.value) {
            SettingsScreen(onCloseClicked = { shouldShowSettings.value = false }, viewModel)
        } else {
            HomeScreen(onSettingsClicked = { shouldShowSettings.value = true })
        }
    }
}

@Composable
fun SettingsScreen(onCloseClicked: () -> Unit, viewModel: SettingsDataViewModel) {

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SlapBlue, SlapBlue2)
                )
            )
    ){
        MyText(
            text = "App Selection",
            fontSize = 25,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp)

        )
        AppSelectionToggle(
            text = "Instragram",
            value = true,
            onValueChanged = {
                scope.launch {
                    viewModel.saveApp("Instagram", it)
                    Log.e("The app is:", viewModel.getApp("Instagram").toString())
                }
            }
        )
    }
        MyIconButton(
            onButtonClicked = onCloseClicked,
            topPadding = 500
        )
}

@Composable
fun AppSelectionToggle(text: String, value: Boolean, onValueChanged: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, start = 50.dp, end = 50.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontFamily = alfaSlabOneFont,
            modifier = Modifier
                .fillMaxWidth(),
            color = Color.White
        )
        Switch(
            checked = value,
            onCheckedChange = onValueChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 500.dp),
            //colors = SwitchDefaults.colors(checkedTrackColor = SlapBlue2)
        )
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
fun MyText(text: String, fontSize: Int, modifier: Modifier) {
    Text (
        text = text,
        textAlign = TextAlign.Center,
        fontSize = fontSize.sp,
        fontFamily = alfaSlabOneFont,
        modifier = modifier,
        color = Color.White,
    )
}

@Composable
fun HomeScreen(onSettingsClicked: () -> Unit) {
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
        MyText(
            text = "Slapp!",
            fontSize = 75,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp, bottom = 75.dp)
        )
        MyText(
            text = "Tap to $status",
            fontSize = 25,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 50.dp)
        )
        Image(
            painter = painterResource(myImage),
            contentDescription = null,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(bottom = 100.dp)
                .clickable { isActive.value = !isActive.value }
        )
        MyIconButton(
            onButtonClicked = onSettingsClicked,
            topPadding = 0
        )
    }
}

////@Preview(showBackground = true)
//@PreviewParameter(viewModel: SettingsDataViewModel)
//@Composable
//fun HomeScreenPreview(viewModel: SettingsDataViewModel) {
//    SlappTheme {
//        MyApp(viewModel)
//    }
//}