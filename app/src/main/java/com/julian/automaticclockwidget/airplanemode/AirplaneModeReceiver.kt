package com.julian.automaticclockwidget.airplanemode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.julian.automaticclockwidget.observability.ObservabilityRepository
import com.julian.automaticclockwidget.workers.AirplaneModeScheduler
import com.julian.automaticclockwidget.workers.CalendarRefreshWorker.Companion.UNIQUE_ONE_TIME_WORK_NAME
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AirplaneModeReceiver : BroadcastReceiver(), KoinComponent {

    private val observability: ObservabilityRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
            val isAirplaneModeOn = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) != 0

            observability.log(
                message = "Airplane mode changed",
                category = "airplane_mode",
                data = mapOf("isOn" to isAirplaneModeOn),
            )

            if (!isAirplaneModeOn) {
                observability.log(
                    message = "Airplane mode off; enqueuing refresh",
                    category = "airplane_mode",
                )
                val request = AirplaneModeScheduler.createOneTimeRequestForRefreshTimezones()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    uniqueWorkName = UNIQUE_ONE_TIME_WORK_NAME,
                    existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request = request
                )
            }
        }
    }

    companion object {
        private const val TAG = "AirplaneModeReceiver"
    }
}
