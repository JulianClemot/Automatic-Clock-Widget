package com.julian.automaticclockwidget.airplanemode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.julian.automaticclockwidget.workers.AirplaneModeScheduler
import com.julian.automaticclockwidget.workers.CalendarRefreshWorker.Companion.UNIQUE_ONE_TIME_WORK_NAME

class AirplaneModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
            val isAirplaneModeOn = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) != 0

            if (!isAirplaneModeOn) {
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