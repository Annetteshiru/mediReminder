package com.example.medireminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_ID = "medication_reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to take your medications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminders(context: Context, medication: MedicationModel) {
        if (medication.id.isEmpty() || medication.frequency == "As needed") return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        medication.times.forEachIndexed { timeIndex, timeStr ->
            // Notification at the exact scheduled time (offset = 0)
            scheduleAlarm(context, alarmManager, medication, timeStr, timeIndex, 0L)
            // Notification for each reminder offset (e.g. 60, 30, 5 min before)
            medication.reminderOffsets.forEach { offsetMinutes ->
                scheduleAlarm(context, alarmManager, medication, timeStr, timeIndex, offsetMinutes)
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        medication: MedicationModel,
        timeStr: String,
        timeIndex: Int,
        offsetMinutes: Long
    ) {
        val triggerTime = getNextAlarmTime(timeStr, offsetMinutes)
        val requestCode = getAlarmRequestCode(medication.id, timeIndex, offsetMinutes)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("medicationId", medication.id)
            putExtra("medicationName", medication.name)
            putExtra("dosage", medication.dosage)
            putExtra("scheduledTime", timeStr)
            putExtra("offsetMinutes", offsetMinutes)
            putExtra("timeIndex", timeIndex)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> alarmManager.canScheduleExactAlarms()
            else -> true  // API < 31: no runtime permission needed
        }

        if (canScheduleExact) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                    )
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            // Fallback to inexact alarm if permission not granted
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelReminders(context: Context, medication: MedicationModel) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        medication.times.forEachIndexed { timeIndex, _ ->
            cancelAlarm(context, alarmManager, medication.id, timeIndex, 0L)
            medication.reminderOffsets.forEach { offsetMinutes ->
                cancelAlarm(context, alarmManager, medication.id, timeIndex, offsetMinutes)
            }
        }
    }

    private fun cancelAlarm(
        context: Context,
        alarmManager: AlarmManager,
        medicationId: String,
        timeIndex: Int,
        offsetMinutes: Long
    ) {
        val requestCode = getAlarmRequestCode(medicationId, timeIndex, offsetMinutes)
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun getNextAlarmTime(timeStr: String, offsetMinutes: Long): Long {
        val parts = timeStr.trim().split(" ")
        val timeParts = parts[0].split(":")
        var hour = timeParts[0].toIntOrNull() ?: 8
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
        val amPm = parts.getOrElse(1) { "AM" }

        if (amPm == "PM" && hour != 12) hour += 12
        if (amPm == "AM" && hour == 12) hour = 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -offsetMinutes.toInt())
        }

        // If the computed time has already passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    fun getAlarmRequestCode(medicationId: String, timeIndex: Int, offsetMinutes: Long): Int {
        val offsetIndex = when (offsetMinutes) {
            0L -> 0; 5L -> 1; 30L -> 2; 60L -> 3; else -> 4
        }
        // Spread across 20 slots per medication (4 times × 5 offsets)
        return (medicationId.hashCode() and 0x7FFFF) * 20 + timeIndex * 5 + offsetIndex
    }
}
