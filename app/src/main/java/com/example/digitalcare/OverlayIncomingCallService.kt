package com.example.digitalcare

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.*

// Serwis typu Foreground - musi działać w tle, aby obsłużyć przychodzące połączenie nawet gdy aplikacja jest zamknięta
class OverlayIncomingCallService : Service(), TextToSpeech.OnInitListener {

    private var number: String = "Nieznany numer"
    private var contactName: String? = null
    private var overlayShown = false

    // Moduł TTS (Text To Speech) do czytania nazwy kontaktu
    private var tts: TextToSpeech? = null
    private var ttsTextToSpeak: String? = null
    private var isTtsReady = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel() // Wymagane od Androida 8.0
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("pl", "PL"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Język polski nieobsługiwany")
            } else {
                isTtsReady = true
                ttsTextToSpeak?.let { speakOut(it) }
            }
        } else {
            Log.e("TTS", "Błąd inicjalizacji TTS")
        }
    }

    private fun speakOut(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "CallID")
        } else {
            ttsTextToSpeak = text
        }
    }

    private fun stopSpeaking() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rawNumber = intent?.getStringExtra("NUMBER")

        // Walidacja numeru przychodzącego
        if (rawNumber.isNullOrBlank() || rawNumber.equals("Nieznany", true)) {
            return START_NOT_STICKY
        }

        number = rawNumber
        contactName = findContactName(number)
        val displayText = contactName ?: number

        // Konfiguracja powiadomienia dla Foreground Service
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "calls")
                .setContentTitle("Połączenie przychodzące")
                .setContentText(displayText)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Połączenie przychodzące")
                .setContentText(displayText)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .build()
        }
        startForeground(1, notification)

        // Wyświetlenie nakładki zamiast standardowego ekranu Androida
        if (!overlayShown) {
            overlayShown = true
            OverlayManager.showOverlay(
                context = this,
                name = (contactName ?: "DZWONI...").uppercase(),
                number = number,
                onAnswer = { answerCall() },
                onDecline = { declineCall() }
            )

            // Powiadomienie głosowe
            val speechText = if (contactName != null) "Dzwoni $contactName" else "Dzwoni nieznany numer"
            speakOut(speechText)
        }

        // Zabezpieczenie: automatyczne zamknięcie serwisu po 2 minutach, gdyby coś się zacięło
        Handler(Looper.getMainLooper()).postDelayed({
            cleanupAndStop()
        }, 120_000)

        return START_NOT_STICKY
    }

    // Programowe odbieranie połączenia (wymaga Android 8.0+ i uprawnień)
    private fun answerCall() {
        stopSpeaking()
        try {
            val telecom = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    telecom.acceptRingingCall()
                }
            }
        } catch (e: Exception) {
            Log.e("OverlayService", "Błąd odbierania: ${e.message}")
        }
        openCallActivity()
        cleanupAndStop()
    }

    // Programowe odrzucanie połączenia
    private fun declineCall() {
        stopSpeaking()
        try {
            val telecom = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    telecom.endCall()
                }
            }
        } catch (e: Exception) {
            Log.e("OverlayService", "Błąd odrzucania: ${e.message}")
        }
        cleanupAndStop()
    }

    private fun cleanupAndStop() {
        OverlayManager.removeOverlay(this)
        overlayShown = false
        stopSelf()
    }

    // Prosta normalizacja numeru telefonu (usuwanie spacji, prefiksu +48)
    private fun normalizeNumber(num: String): String {
        return num.replace("[^0-9]".toRegex(), "")
            .removePrefix("48")
            .removePrefix("0048")
            .trim()
    }

    // Wyszukiwanie kontaktu w lokalnej bazie SharedPreferences
    private fun findContactName(incomingNumber: String): String? {
        val prefs = applicationContext.getSharedPreferences("contacts", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("contacts_set", null) ?: return null
        val cleanIncoming = normalizeNumber(incomingNumber)
        for (entry in set) {
            val parts = entry.split(" - ")
            if (parts.size == 2) {
                val name = parts[0]
                val storedNumber = normalizeNumber(parts[1])
                // Porównujemy tylko ostatnie 7 cyfr
                if (cleanIncoming.takeLast(7) == storedNumber.takeLast(7)) {
                    return name
                }
            }
        }
        return null
    }

    private fun openCallActivity() {
        val i = Intent(this, CallActivity::class.java).apply {
            // Flagi wymagane przy uruchamianiu Activity z poziomu serwisu
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("NUMBER", number)
            putExtra("INCOMING", true)
            contactName?.let { putExtra("NAME", it) }
        }
        startActivity(i)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("calls", "Połączenia", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        OverlayManager.removeOverlay(this)
        overlayShown = false
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}