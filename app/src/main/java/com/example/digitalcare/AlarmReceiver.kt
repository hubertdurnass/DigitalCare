package com.example.digitalcare

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Odbierz sygnał alarmowy
        val reminderName = intent.getStringExtra("REMINDER_NAME") ?: "Przypomnienie"

        // Uruchom ekran AlarmActivity, aby obudzić użytkownika
        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("REMINDER_NAME", reminderName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(activityIntent)
    }
}