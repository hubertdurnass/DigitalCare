package com.example.digitalcare

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

// Helper object do zarządzania harmonogramem alarmów
object AlarmScheduler {

    fun schedule(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("REMINDER_NAME", reminder.name)
        }

        // FLAG_IMMUTABLE wymagane w Android (12+)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
        }

        // Jeśli godzina już minęła, ustawiamy na jutro
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            // Używamy setExactAndAllowWhileIdle, aby alarm zadziałał nawet w trybie uśpienia (Doze Mode).
            if (reminder.days.isEmpty()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                // Dla alarmów powtarzalnych
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
            Log.d("AlarmScheduler", "Ustawiono alarm: ${reminder.name} na ${reminder.hour}:${reminder.minute}")

        } catch (e: SecurityException) {
            // Obsługa braku uprawnień SCHEDULE_EXACT_ALARM w Android 12+
            Log.e("AlarmScheduler", "Brak uprawnień do ustawienia alarmu: ${e.message}")
        }
    }

    fun cancel(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}