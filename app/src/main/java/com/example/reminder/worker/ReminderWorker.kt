package com.example.reminder.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.reminder.MainActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val onlyCharging = inputData.getBoolean("onlyCharging", false)
        val onlyFullBattery = inputData.getBoolean("onlyFullBattery", false)

        val batteryStatus = applicationContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (scale > 0) level * 100 / scale else 0
        val isCharging = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0) != 0

        if (onlyCharging && !isCharging) return Result.retry()
        if (onlyFullBattery && batteryPct < 100) return Result.retry()

        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        val channelId = "reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Reminders", NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(applicationContext, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            applicationContext, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(applicationContext, NotificationReceiver::class.java).apply {
            action = "STOP_WORK"
        }
        val stopPending = PendingIntent.getBroadcast(
            applicationContext, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val nextRun = Calendar.getInstance().apply {
            add(Calendar.MINUTE, inputData.getInt("interval", 15))
        }
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(nextRun.time)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Пора сделать перерыв ☕")
            .setContentText("Следующее напоминание в $time")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_menu_view, "Открыть", openPending)
            .addAction(android.R.drawable.ic_delete, "Стоп", stopPending)
            .setAutoCancel(true)
            .build()

        manager.notify(1, notification)
    }
}
