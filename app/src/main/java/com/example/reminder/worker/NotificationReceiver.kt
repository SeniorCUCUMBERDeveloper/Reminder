package com.example.reminder.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "STOP_WORK") {
            WorkManager.getInstance(context).cancelAllWorkByTag("periodic")
        }
    }
}
