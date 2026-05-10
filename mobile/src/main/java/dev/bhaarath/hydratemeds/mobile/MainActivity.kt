package dev.bhaarath.hydratemeds.mobile

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import dagger.hilt.android.AndroidEntryPoint
import dev.bhaarath.hydratemeds.mobile.ui.HydrateMedsApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val waterConfirmationTrigger = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeRequestExactAlarmAccess()
        consumeWaterConfirmationIntent(intent)

        setContent {
            val notificationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) {}

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            HydrateMedsApp(
                debugWaterConfirmationTrigger = waterConfirmationTrigger.intValue,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeWaterConfirmationIntent(intent)
    }

    private fun consumeWaterConfirmationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_SHOW_WATER_CONFIRMATION, false) == true) {
            waterConfirmationTrigger.intValue += 1
        }
    }

    private fun maybeRequestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < 31) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmManager.canScheduleExactAlarms()) return

        startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            },
        )
    }

    companion object {
        const val EXTRA_SHOW_WATER_CONFIRMATION =
            "dev.bhaarath.hydratemeds.extra.SHOW_WATER_CONFIRMATION"
    }
}
