package com.example.slapp

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.core.Preferences
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.slapp.ui.theme.SlapBlue
import com.example.slapp.ui.theme.SlapBlue2
import com.example.slapp.ui.theme.SlappTheme
import com.example.slapp.ui.theme.SliderBLue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.reflect.KFunction4


const val CHANNEL_ID = "1"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel by viewModels<SettingsDataViewModel>()

    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

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

        val workRepeatInterval: Duration = Duration.ofMinutes(15)
        val workFlexInterval: Duration = Duration.ofMinutes(14)

        setContent {
            SlappTheme {
                // A surface container using the 'background' color from the theme
                MyApp(
                    modifier = Modifier.fillMaxSize(), settingsViewModel, ::createBackgroundWorker,
                installedAppList, workRepeatInterval, workFlexInterval, ::killBackgroundWorker,
                ::getImageFromPackageName, ::getAppNameFromPackageName,
                    ::requestUsageStatsPermission, ::requestNotificationPermission)
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

    // Create worker to monitor restricted apps and send slap notifications, even after the
    // app is closed
    private fun createBackgroundWorker(installedAppList: ArrayList<String>,
                                       workRepeatInterval: Duration, workFlexInterval: Duration,
                                       existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy) {
        requestNotificationPermission()
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
            .getInstance(applicationContext).enqueueUniquePeriodicWork(
                "SlapWorker",
                existingPeriodicWorkPolicy, notificationWorkRequest
            )
    }

    // Cancel all background workers
    private fun killBackgroundWorker() {
        Log.e("Its off", "Its OFF!")
        WorkManager.getInstance(applicationContext).cancelAllWork()
    }

    private fun getImageFromPackageName(packageName: String): ImageBitmap {
        val drawable = packageManager.getApplicationIcon(packageName)
        val bitMap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitMap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitMap.asImageBitmap()
    }

    private fun getAppNameFromPackageName(packageName: String): String {
        return packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName,
            PackageManager.ApplicationInfoFlags.of(0))).toString()
    }

    private fun requestUsageStatsPermission(): Boolean {
        return if (!hasUsageStatsPermission(this)
        ) {
            Toast.makeText(applicationContext, "Slapp! requires usage access permissions."
                , Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            // Below statement provides the return value
            hasUsageStatsPermission(this)
        } else {
            true
        }
    }

    private fun requestNotificationPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(applicationContext, "Slapp! requires notification permissions."
                , Toast.LENGTH_LONG).show()
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            // Below statement provides the return value
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps =
            context.getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            "android:get_usage_stats",
            Process.myUid(), context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

@Composable
private fun MyApp(
    modifier: Modifier = Modifier,
    viewModel: SettingsDataViewModel,
    createBackgroundWorker: KFunction4<ArrayList<String>, Duration, Duration, ExistingPeriodicWorkPolicy, Unit>,
    installedAppList: ArrayList<String>,
    workRepeatInterval: Duration,
    workFlexInterval: Duration,
    killBackgroundWorker: () -> Unit,
    getImageFromPackageName: (String) -> ImageBitmap,
    getAppNameFromPackageName: (String) -> String,
    requestUsageStatsPermission: () -> Boolean,
    requestNotificationPermission: () -> Boolean
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
            }, viewModel, savedAppList, getImageFromPackageName, getAppNameFromPackageName)
        } else {
            HomeScreen(
                onSettingsClicked = {
                scope.launch {
                    savedAppList = viewModel.getAppNames()
                }
                shouldShowSettings.value = true
            }, viewModel, createBackgroundWorker, installedAppList, workRepeatInterval,
                workFlexInterval, killBackgroundWorker,
                startingValue = viewModel.getApp("isActive") == true,
                requestUsageStatsPermission, requestNotificationPermission)
        }
    }
}

@Composable
fun SettingsScreen(
    onCloseClicked: () -> Unit, viewModel: SettingsDataViewModel,
    savedAppList: Set<Preferences.Key<*>>?,
    getImageFromPackageName: (String) -> ImageBitmap,
    getAppNameFromPackageName: (String) -> String
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
                        .padding(top = 130.dp, bottom = 30.dp)
                )
            }
            if (savedAppList != null) {
                for (app in savedAppList) {
                    if (app.name != "isActive") {
                        val appIcon = getImageFromPackageName(app.name)
                        val appName = getAppNameFromPackageName(app.name)

                        item {
                            AppSelectionToggle(
                                appName,
                                appIcon,
                                app.name,
                                viewModel,
                                scope,
                                startingValue = viewModel.getApp(app.name) == true
                            )
                        }
                    }
                }
            }
        }
        MyIconButton(
            onButtonClicked = onCloseClicked,
            topPadding = 40,
            icon = R.drawable.applybutton,
            modifier = Modifier.size(size = 100.dp)
        )
    }
}

@Composable
fun AppSelectionToggle(
    appName: String,
    appIcon: ImageBitmap,
    packageName: String,
    viewModel: SettingsDataViewModel,
    scope: CoroutineScope,
    startingValue: Boolean
) {

    val isAppActive = remember { mutableStateOf(startingValue) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, start = 50.dp, end = 50.dp),
        verticalAlignment = Alignment.CenterVertically

    ) {
        Image(
            appIcon,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 42.dp)
                .width(60.dp)
                .height(60.dp)
        )

        Text(
            text = appName,
            fontSize = 20.sp,
            modifier = Modifier
                .width(180.dp)
                .padding(end = 42.dp)
                .fillMaxHeight(),
            color = Color.White
        )

        Switch(
            checked = isAppActive.value,
            modifier = Modifier
                .padding(end = 30.dp, top = 10.dp)
                .fillMaxHeight(),
            onCheckedChange = {
                scope.launch {
                    viewModel.saveApp(packageName, !isAppActive.value)
                    isAppActive.value = !isAppActive.value
                }
            },
            colors = SwitchDefaults.colors(checkedTrackColor = SliderBLue)
        )
    }
}

@Composable
fun MyIconButton(onButtonClicked: () -> Unit, topPadding: Int, icon: Int, modifier: Modifier) {
    IconButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding.dp),
        onClick = onButtonClicked
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = modifier,
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
    killBackgroundWorker: () -> Unit,
    startingValue: Boolean,
    requestUsageStatsPermission: () -> Boolean,
    requestNotificationPermission: () -> Boolean
) {
    val isActive = remember { mutableStateOf(startingValue) }
    val status = if (isActive.value) "Deactivate" else "Activate"
    val myImage = if (isActive.value) R.drawable.openhandbutton else R.drawable.closedfistbutton
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
            if (requestNotificationPermission() && requestUsageStatsPermission()) {
                if (isActive.value) {
                    createBackgroundWorker(
                        installedAppList, workRepeatInterval, workFlexInterval,
                        ExistingPeriodicWorkPolicy.UPDATE
                    )
                } else {
                    killBackgroundWorker()
                }
            } else {
                isActive.value = false
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
                        if (requestUsageStatsPermission() && requestNotificationPermission()) {
                            viewModel.saveApp("isActive", !isActive.value)
                            isActive.value = !isActive.value
                            if (isActive.value) {
                                createBackgroundWorker(
                                    installedAppList, workRepeatInterval,
                                    workFlexInterval, ExistingPeriodicWorkPolicy.UPDATE
                                )
                            } else {
                                killBackgroundWorker()
                            }
                        }
                    }
                }
        )
        MyIconButton(
            onButtonClicked = onSettingsClicked,
            topPadding = 0,
            icon = R.drawable.settingsbutton,
            modifier = Modifier.size(size = 45.dp)
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