package com.example.slapp

import android.app.NotificationManager
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
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
import kotlinx.coroutines.CoroutineScope
import java.time.Duration
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction4

const val CHANNEL_ID = "1"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel by viewModels<SettingsDataViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get relevant installed applications
        val installedAppList = getInstalledApplications()

        // Populate settings with newly installed apps
        for (appName in installedAppList) {
            if (settingsViewModel.getApp(appName) == null) {
                settingsViewModel.saveApp(appName, true)
            }
        }

        // Create notification channel to send slaps.
        setUpSlapNotificationChannel()

        // Create worker to monitor restricted apps and send slap notifications, even after the
        // app is closed
        val workRepeatInterval: Duration = Duration.ofMinutes(15)
        val workFlexInterval: Duration = Duration.ofMinutes(14)

        setContent {
            SlappTheme {
                // A surface container using the 'background' color from the theme
                MyApp(
                    modifier = Modifier.fillMaxSize(), settingsViewModel, ::createBackgroundWorker,
                installedAppList, workRepeatInterval, workFlexInterval, ::killBackgroundWorker)
            }
        }
    }

    private fun getAppsToMonitor(appList: ArrayList<String>):
            ArrayList<String> {
        val appsToMonitor = arrayListOf<String>()
        for (appName in appList) {
            if (appName != "isActive" && settingsViewModel.getApp(appName) == true) {
                appsToMonitor.add(appName)
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

    private fun createBackgroundWorker(installedAppList: ArrayList<String>,
                                       workRepeatInterval: Duration, workFlexInterval: Duration,
                                       existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy) {
        Log.e("Its on", "Its ON!")
        val appsToMonitor = getAppsToMonitor(installedAppList)
        for (i in appsToMonitor) {
            Log.e("My apps", i)
        }
        val data = Data.Builder()
        data.putStringArray("appsToMonitor", appsToMonitor.toTypedArray())

        val notificationWorkRequest: PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<SlapWorker>(workRepeatInterval, workFlexInterval)
                .setInputData(data.build()).build()

        WorkManager
            .getInstance(applicationContext).enqueueUniquePeriodicWork("SlapWorker",
                existingPeriodicWorkPolicy, notificationWorkRequest)
    }

    private fun killBackgroundWorker() {
        Log.e("Its off", "Its OFF!")
        WorkManager.getInstance(applicationContext).cancelAllWork()
    }
}

@Composable
private fun MyApp(
    modifier: Modifier = Modifier,
    viewModel: SettingsDataViewModel,
    createBackgroundWorker: KFunction4<ArrayList<String>, Duration, Duration,
            ExistingPeriodicWorkPolicy, Unit>,
    installedAppList: ArrayList<String>,
    workRepeatInterval: Duration,
    workFlexInterval: Duration,
    killBackgroundWorker: () -> Unit,
) {

    val shouldShowSettings = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var savedAppList: Set<Preferences.Key<*>>? = setOf()

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        if (shouldShowSettings.value) {
            SettingsScreen(
                onCloseClicked = {
                shouldShowSettings.value = false
            }, viewModel, savedAppList)
        } else {
            HomeScreen(
                onSettingsClicked = {
                scope.launch {
                    savedAppList = viewModel.getAppNames()
                }
                shouldShowSettings.value = true
            }, viewModel, createBackgroundWorker, installedAppList, workRepeatInterval,
                workFlexInterval, killBackgroundWorker)
        }
    }
}

@Composable
fun SettingsScreen(
    onCloseClicked: () -> Unit, viewModel: SettingsDataViewModel,
    savedAppList: Set<Preferences.Key<*>>?
) {

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SlapBlue, SlapBlue2)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .height(592.dp)
        ) {
            item {
                MyText(
                    text = "App Selection",
                    fontSize = 25,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp)
                )
            }
            if (savedAppList != null) {
                for (app in savedAppList) {
                    if (app.name != "isActive") {
                        item {
                            AppSelectionToggle(
                                app.name,
                                viewModel,
                                scope
                            )
                        }
                    }
                }
            }
        }
        MyIconButton(
            onButtonClicked = onCloseClicked,
            topPadding = 40
        )
    }
}

@Composable
fun AppSelectionToggle(
    appName: String,
    viewModel: SettingsDataViewModel,
    scope: CoroutineScope,
) {

    val isAppActive = remember { mutableStateOf(true) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, start = 50.dp, end = 50.dp)
    ) {
        Text(
            text = appName,
            fontSize = 20.sp,
            fontFamily = alfaSlabOneFont,
            modifier = Modifier
                .padding(end = 50.dp),
            color = Color.White
        )

        LaunchedEffect(viewModel) {
            isAppActive.value = viewModel.getApp(appName) == true
        }

        Switch(
            checked = isAppActive.value,
            modifier = Modifier.padding(end = 50.dp),
            onCheckedChange = {
                scope.launch {
                    viewModel.saveApp(appName, !isAppActive.value)
                    isAppActive.value = !isAppActive.value
                    // Make back button "Apply" so application of changes is done by returning to the home screen
                }
            }
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
fun HomeScreen(
    onSettingsClicked: () -> Unit,
    viewModel: SettingsDataViewModel,
    createBackgroundWorker: KFunction4<ArrayList<String>, Duration, Duration, ExistingPeriodicWorkPolicy, Unit>,
    installedAppList: ArrayList<String>,
    workRepeatInterval: Duration,
    workFlexInterval: Duration,
    killBackgroundWorker: () -> Unit
) {
    val isActive = remember { mutableStateOf(false) }
    val status = if (isActive.value) "Deactivate" else "Activate"
    val myImage = if (isActive.value) R.drawable.openhandbutton else R.drawable.closedfistbutton

    val scope = rememberCoroutineScope()

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

        LaunchedEffect(viewModel) {
            isActive.value = viewModel.getApp("isActive") == true
            if (isActive.value) {
                createBackgroundWorker(installedAppList, workRepeatInterval, workFlexInterval,
                    ExistingPeriodicWorkPolicy.UPDATE)
            } else {
                killBackgroundWorker()
            }
        }

        Image(
            painter = painterResource(myImage),
            contentDescription = null,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(bottom = 100.dp)
                .clickable {
                    scope.launch {
                        viewModel.saveApp("isActive", !isActive.value)
                        isActive.value = !isActive.value
                        if (isActive.value) {
                            createBackgroundWorker(installedAppList, workRepeatInterval,
                                workFlexInterval, ExistingPeriodicWorkPolicy.UPDATE)
                        } else {
                            killBackgroundWorker()
                        }
                    }
                }
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