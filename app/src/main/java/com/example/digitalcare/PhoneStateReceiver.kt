package com.example.digitalcare

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val number = intent.getStringExtra("incoming_number") ?: "Nieznany"

        Log.d("Cyfrowy Opiekun", "📞 Zmiana stanu: $state | Numer: $number")

        // Zapis ostatniego numeru w preferencjach
        context.getSharedPreferences("last_call", Context.MODE_PRIVATE)
            .edit()
            .putString("incoming_number", number)
            .apply()

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Uruchom nakładkę
                val overlayIntent = Intent(context, OverlayIncomingCallService::class.java).apply {
                    putExtra("NUMBER", number)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    ContextCompat.startForegroundService(context, overlayIntent)
                } catch (e: Exception) {
                    Log.e("Cyfrowy Opiekun", "❌ Błąd uruchamiania OverlayIncomingCallService: ${e.message}")
                    context.startService(overlayIntent)
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK,
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Zatrzymaj nakładkę jeśli istnieje
                try {
                    val stopIntent = Intent(context, OverlayIncomingCallService::class.java)
                    context.stopService(stopIntent)
                    Log.d("Cyfrowy Opiekun", "🧹 Zatrzymano OverlayIncomingCallService po zmianie stanu")
                } catch (e: Exception) {
                    Log.e("Cyfrowy Opiekun", "❌ Nie udało się zatrzymać OverlayIncomingCallService: ${e.message}")
                }
            }
        }
    }
}
