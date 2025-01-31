package com.lorenzovainigli.foodexpirationdates.view.activity

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lorenzovainigli.foodexpirationdates.BuildConfig
import com.lorenzovainigli.foodexpirationdates.R
import com.lorenzovainigli.foodexpirationdates.model.PreferencesProvider
import com.lorenzovainigli.foodexpirationdates.model.worker.CheckExpirationsWorker
import com.lorenzovainigli.foodexpirationdates.ui.theme.FoodExpirationDatesTheme
import com.lorenzovainigli.foodexpirationdates.ui.theme.TonalElevation
import com.lorenzovainigli.foodexpirationdates.view.composable.DateFormatDialog
import com.lorenzovainigli.foodexpirationdates.view.composable.MyTopAppBar
import com.lorenzovainigli.foodexpirationdates.view.composable.SettingsItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class SettingsActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsActivityLayout()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @Preview
    fun SettingsActivityLayout() {
        val context = LocalContext.current
        val activity = (LocalContext.current as? Activity)
        var dateFormat = PreferencesProvider.getUserDateFormat(context)
        var sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
        var isDateFormatDialogOpened by remember {
            mutableStateOf(false)
        }
        val notificationTimeHour = PreferencesProvider.getUserNotificationTimeHour(context)
        val notificationTimeMinute = PreferencesProvider.getUserNotificationTimeMinute(context)
        val timePickerState =
            rememberTimePickerState(notificationTimeHour, notificationTimeMinute, true)
        var isNotificationTimeBottomSheetOpen by remember {
            mutableStateOf(false)
        }
        var darkThemeState by remember {
            mutableStateOf(PreferencesProvider.getThemeMode(context))
        }
        var dynamicColorsState by remember {
            mutableStateOf(PreferencesProvider.getDynamicColors(context))
        }
        FoodExpirationDatesTheme(
            darkTheme = when (darkThemeState) {
                PreferencesProvider.Companion.ThemeMode.LIGHT.ordinal -> false
                PreferencesProvider.Companion.ThemeMode.DARK.ordinal -> true
                else -> isSystemInDarkTheme()
            },
            dynamicColor = dynamicColorsState
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = TonalElevation.level2()
            ) {
                Scaffold(
                    topBar = {
                        MyTopAppBar(
                            title = stringResource(id = R.string.settings),
                            navigationIcon = {
                                IconButton(onClick = { activity?.finish() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowBack,
                                        contentDescription = stringResource(id = R.string.back),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        )
                    }
                ) { padding ->
                    DateFormatDialog(
                        isDialogOpen = isDateFormatDialogOpened,
                        onDismissRequest = {
                            dateFormat = PreferencesProvider.getUserDateFormat(context)
                            sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
                            isDateFormatDialogOpened = false
                        }
                    )
                    if (isNotificationTimeBottomSheetOpen) {
                        ModalBottomSheet(
                            sheetState = rememberModalBottomSheetState(
                                skipPartiallyExpanded = true
                            ),
                            content = {
                                Column(
                                    modifier = Modifier
                                        .align(CenterHorizontally)
                                        .padding(4.dp)
                                ) {
                                    TimePicker(
                                        state = timePickerState
                                    )
                                }
                            },
                            onDismissRequest = {
                                PreferencesProvider.setUserNotificationTime(
                                    context,
                                    timePickerState.hour, timePickerState.minute
                                )
                                scheduleDailyNotification(
                                    timePickerState.hour,
                                    timePickerState.minute
                                )
                                isNotificationTimeBottomSheetOpen = false
                            }
                        )
                    }
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SettingsItem(
                            label = stringResource(id = R.string.date_format)
                        ){
                            ClickableText(
                                modifier = Modifier.testTag(stringResource(id = R.string.date_format)),
                                text = AnnotatedString(sdf.format(Calendar.getInstance().time)),
                                style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                onClick = {
                                    isDateFormatDialogOpened = true
                                }
                            )
                        }
                        SettingsItem(
                            label = stringResource(R.string.notification_time)
                        ){
                            var text = ""
                            if (timePickerState.hour < 10) {
                                text += "0"
                            }
                            text = timePickerState.hour.toString() + ":"
                            if (timePickerState.minute < 10) {
                                text += "0"
                            }
                            text += timePickerState.minute.toString()
                            ClickableText(
                                modifier = Modifier.testTag("Notification time"),
                                text = AnnotatedString(text),
                                style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                onClick = {
                                    isNotificationTimeBottomSheetOpen = true
                                }
                            )
                        }
                        SettingsItem(
                            label = stringResource(R.string.theme)
                        ){
                            PreferencesProvider.Companion.ThemeMode.values().forEach {
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(0.1f)
                                )
                                if (it.ordinal == darkThemeState) {
                                    Button(onClick = {}) {
                                        Text(
                                            text = getString(it.label)
                                        )
                                    }
                                }
                                if(it.ordinal != darkThemeState){
                                    OutlinedButton(
                                        onClick = {
                                            darkThemeState = it.ordinal
                                            PreferencesProvider.setThemeMode(context, it)
                                        },
                                    ) {
                                        Text(
                                            text = getString(it.label)
                                        )
                                    }
                                }
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(0.1f)
                                )
                            }
                        }
                        SettingsItem(
                            label = stringResource(R.string.dynamic_colors)
                        ){
                            Spacer(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight())
                            Switch(
                                checked = dynamicColorsState,
                                onCheckedChange = {
                                    PreferencesProvider.setDynamicColors(context, it)
                                    dynamicColorsState = it
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun scheduleDailyNotification(hour: Int, minute: Int) {
        val currentTime = Calendar.getInstance()
        val dueTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        if (currentTime > dueTime)
            dueTime.add(Calendar.DAY_OF_MONTH, 1)
        val initialDelay = dueTime.timeInMillis - currentTime.timeInMillis
        val formattedTime = formatTimeDifference(initialDelay)
        if (BuildConfig.DEBUG) {
            Toast.makeText(
                applicationContext,
                "Notification in $formattedTime",
                Toast.LENGTH_SHORT
            ).show()
        }
        val workRequest = PeriodicWorkRequestBuilder<CheckExpirationsWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                CheckExpirationsWorker.workerID,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                workRequest
            )
    }

    private fun formatTimeDifference(timeDifference: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(timeDifference)
        val hours = TimeUnit.MILLISECONDS.toHours(timeDifference) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeDifference) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeDifference) % 60
        val formattedTime = StringBuilder()
        if (days > 0) {
            formattedTime.append("$days day${if (days > 1) "s" else ""} ")
        }
        if (hours > 0) {
            formattedTime.append("$hours hour${if (hours > 1) "s" else ""} ")
        }
        if (minutes > 0) {
            formattedTime.append("$minutes minute${if (minutes > 1) "s" else ""} ")
        }
        if (seconds > 0) {
            formattedTime.append("$seconds second${if (seconds > 1) "s" else ""} ")
        }
        return formattedTime.toString().trim()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
    @Composable
    fun PreviewDarkMode() {
        SettingsActivityLayout()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Preview(name = "Italian", locale = "it", showBackground = true)
    @Composable
    fun PreviewItalian() {
        SettingsActivityLayout()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Preview(name = "Arabic", locale = "ar", showBackground = true)
    @Composable
    fun PreviewArabic() {
        SettingsActivityLayout()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Preview(name = "German", locale = "de", showBackground = true)
    @Composable
    fun PreviewGerman() {
        SettingsActivityLayout()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Preview(name = "Hindi", locale = "hi", showBackground = true)
    @Composable
    fun PreviewHindi() {
        SettingsActivityLayout()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Preview(name = "Spanish", locale = "es", showBackground = true)
    @Composable
    fun PreviewSpanish() {
        SettingsActivityLayout()
    }
}