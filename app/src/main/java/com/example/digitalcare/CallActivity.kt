package com.example.digitalcare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class CallActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvNumber: TextView
    private lateinit var btnHangup: Button

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var callStateListener: PhoneStateListener
    private var callStarted = true

    private val focusHandler = Handler(Looper.getMainLooper())
    private var focusRunnable: Runnable? = null
    private var focusTryCount = 0
    private val MAX_FOCUS_TRIES = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        tvName = findViewById(R.id.tvName)
        tvNumber = findViewById(R.id.tvNumber)
        btnHangup = findViewById(R.id.btnHangup)

        // Odbieramy dane z DialingActivity
        val number = intent.getStringExtra("NUMBER") ?: ""
        val contactName = intent.getStringExtra("NAME") ?: findContactName(number) // Zapasowe wyszukiwanie

        tvName.text = contactName ?: "Nieznany kontakt"
        tvNumber.text = number

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // Nasłuchujemy tylko końca rozmowy
        callStateListener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                if (state == TelephonyManager.CALL_STATE_IDLE && callStarted) {
                    Log.d("CallActivity", "Rozmowa zakończona (IDLE), zamykam.")
                    finish()
                }
            }
        }
        @Suppress("DEPRECATION")
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        btnHangup.setOnClickListener { endCall() }

        startFocusWar()
    }

    // Funkcja "walki o focus"
    // Wymusza wyciągnięcie CallActivity na wierzch (FLAG_ACTIVITY_REORDER_TO_FRONT).
    private fun startFocusWar() {
        focusTryCount = 0
        focusRunnable = Runnable {
            if (focusTryCount < MAX_FOCUS_TRIES) {
                val intent = Intent(this, CallActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)

                focusTryCount++
                focusHandler.postDelayed(focusRunnable!!, 700)
            }
        }
        // Uruchomienie z małym opóźnieniem, aby dać czas systemowi na inicjalizację połączenia
        focusHandler.postDelayed(focusRunnable!!, 200)
    }

    // Funkcja rozłączania
    private fun endCall() {
        focusRunnable?.let { focusHandler.removeCallbacks(it) }
        var success = false
        if (CallManager.currentCall != null) {
            try {
                CallManager.hangup()
                success = true
            } catch (e: Exception) { Log.e("CallActivity", "Błąd CallManager: ${e.message}") }
        }
        if (!success) {
            try {
                val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    @Suppress("DEPRECATION")
                    telecomManager.endCall()
                    success = true
                }
            } catch (e: Exception) { Log.e("CallActivity", "Błąd TelecomManager: ${e.message}") }
        }
        if (success) {
            finish()
        } else {
            Toast.makeText(this, "Nie udało się zakończyć połączenia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findContactName(phoneNumber: String): String? {
        val prefs = getSharedPreferences("contacts", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("contacts_set", setOf()) ?: return null
        val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")
        for (entry in set) {
            val parts = entry.split(" - ")
            if (parts.size == 2) {
                val name = parts[0]
                val stored = parts[1].replace(Regex("[^0-9+]"), "")
                if (stored.endsWith(cleaned.takeLast(7))) return name
            }
        }
        return null
    }

    override fun onDestroy() {
        focusRunnable?.let { focusHandler.removeCallbacks(it) }
        @Suppress("DEPRECATION")
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_NONE)
        super.onDestroy()
    }
}