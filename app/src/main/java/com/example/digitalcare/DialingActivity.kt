package com.example.digitalcare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class DialingActivity : AppCompatActivity() {

    private lateinit var tvDialingName: TextView
    private lateinit var tvDialingNumber: TextView
    private var number: String = ""
    private var contactName: String? = ""

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var callStateListener: PhoneStateListener
    private var callInitiated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialing)

        tvDialingName = findViewById(R.id.tvDialingName)
        tvDialingNumber = findViewById(R.id.tvDialingNumber)

        number = intent.getStringExtra("NUMBER") ?: ""
        contactName = findContactName(number)

        tvDialingName.text = contactName ?: "Nieznany"
        tvDialingNumber.text = number

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        setupCallStateListener()

        // Opóźnia dzwonienie, aby ekran zdążył się narysować
        Handler(Looper.getMainLooper()).postDelayed({
            initiateCall(number)
        }, 500)

    }

    private fun setupCallStateListener() {
        callStateListener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        // Odebranie
                        Log.d("DialingActivity", "Rozmowa aktywna (OFFHOOK). Uruchamiam CallActivity.")

                        val intent = Intent(this@DialingActivity, CallActivity::class.java).apply {
                            putExtra("NUMBER", number)
                            putExtra("NAME", contactName)
                            putExtra("INCOMING", false)
                        }
                        startActivity(intent)

                        finish() // Zamknij "poczekalnię"
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        // Rozmowa zakończona (zanim ktoś odebrał lub odrzucił)
                        if (callInitiated) {
                            Log.d("DialingActivity", "Rozmowa zakończona (IDLE). Zamykam.")
                            finish()
                        }
                    }
                }
            }
        }
        @Suppress("DEPRECATION")
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun initiateCall(phoneNumber: String) {
        try {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val uri = Uri.fromParts("tel", phoneNumber, null)

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            telecomManager.placeCall(uri, null)
            callInitiated = true
            Toast.makeText(this, "Dzwonienie...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Nie udało się zainicjować połączenia: ${e.message}", Toast.LENGTH_LONG).show()
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
        @Suppress("DEPRECATION")
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_NONE)
        super.onDestroy()
    }
}