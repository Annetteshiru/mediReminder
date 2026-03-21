package com.example.medireminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getStringExtra("medicationId") ?: return
        val medicationName = intent.getStringExtra("medicationName") ?: "Medication"
        val dosage = intent.getStringExtra("dosage") ?: ""
        val scheduledTime = intent.getStringExtra("scheduledTime") ?: ""
        val offsetMinutes = intent.getLongExtra("offsetMinutes", 0L)
        val timeIndex = intent.getIntExtra("timeIndex", 0)

        val (title, message) = when (offsetMinutes) {
            0L   -> "Time to take $medicationName" to "Take ${dosage}mg now ($scheduledTime)"
            60L  -> "Upcoming medication" to "$medicationName in 1 hour ($scheduledTime)"
            30L  -> "Upcoming medication" to "$medicationName in 30 minutes ($scheduledTime)"
            5L   -> "Almost time!" to "Take $medicationName in 5 minutes"
            else -> "Medication reminder" to "Time to take $medicationName"
        }

        showNotification(context, medicationId, title, message, offsetMinutes)
        rescheduleForNextDay(context, intent, medicationId, scheduledTime, timeIndex, offsetMinutes)
    }

    private fun showNotification(
        context: Context,
        medicationId: String,
        title: String,
        message: String,
        offsetMinutes: Long
    ) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        val notificationId = (medicationId.hashCode() and 0xFFFF) + offsetMinutes.toInt()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun rescheduleForNextDay(
        context: Context,
        originalIntent: Intent,
        medicationId: String,
        scheduledTime: String,
        timeIndex: Int,
        offsetMinutes: Long
    ) {
        // Since the alarm just fired, getNextAlarmTime will return tomorrow's occurrence
        val triggerTime = NotificationHelper.getNextAlarmTime(scheduledTime, offsetMinutes)
        val requestCode = NotificationHelper.getAlarmRequestCode(medicationId, timeIndex, offsetMinutes)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, originalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> alarmManager.canScheduleExactAlarms()
            else -> true
        }
        if (canScheduleExact) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }
}
