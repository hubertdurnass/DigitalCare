package com.example.digitalcare

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import androidx.core.content.ContextCompat

class CallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        // Przekazujemy połączenie do CallManager
        CallManager.updateCall(call)

        // Jeśli telefon dzwoni - uruchamiamy nakładkę
        if (call.state == Call.STATE_RINGING) {
            val intent = Intent(this, OverlayIncomingCallService::class.java)

            // numer dzwoniącego
            val number = call.details.handle?.schemeSpecificPart ?: "Nieznany"
            intent.putExtra("NUMBER", number)

            try {
                ContextCompat.startForegroundService(this, intent)
            } catch (e: Exception) {
                startService(intent)
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.updateCall(null)

        // Jak rozmowa się skończy, wyłączamy nakładkę
        stopService(Intent(this, OverlayIncomingCallService::class.java))
    }
}