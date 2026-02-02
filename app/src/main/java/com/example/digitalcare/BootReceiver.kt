package com.example.digitalcare

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {

            val prefs = context.getSharedPreferences("reminders_prefs", Context.MODE_PRIVATE)
            val gson = Gson()
            val json = prefs.getString("reminders_json", null)
            if (json != null) {
                val type = object : TypeToken<ArrayList<Reminder>>() {}.type
                val reminders: ArrayList<Reminder> = gson.fromJson(json, type)

                for (reminder in reminders) {
                    AlarmScheduler.schedule(context, reminder)
                }
            }
        }
    }
}