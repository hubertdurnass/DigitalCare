package com.example.digitalcare

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class AlarmActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var ringtone: Ringtone? = null


    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var reminderNameForTts: String = "Przypomnienie"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        setContentView(R.layout.activity_alarm)

        val tvAlarmName: TextView = findViewById(R.id.tvAlarmName)
        val btnDismiss: Button = findViewById(R.id.btnDismiss)

        // Zapisujemy nazwę alarmu, aby TTS mógł jej użyć
        reminderNameForTts = intent.getStringExtra("REMINDER_NAME") ?: "Przypomnienie"
        tvAlarmName.text = reminderNameForTts

        tts = TextToSpeech(this, this)

        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(applicationContext, notification)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        btnDismiss.setOnClickListener {
            ringtone?.stop()
            tts?.stop()
            finish()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("pl", "PL"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Język polski nieobsługiwany")
            } else {
                isTtsReady = true
                speakOut("Przypomnienie: $reminderNameForTts")
            }
        } else {
            Log.e("TTS", "Błąd inicjalizacji TTS")
        }
    }

    private fun speakOut(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AlarmID")
        }
    }

    override fun onDestroy() {
        ringtone?.stop()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}