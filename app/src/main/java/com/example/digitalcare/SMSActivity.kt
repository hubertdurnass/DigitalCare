package com.example.digitalcare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SMSActivity : AppCompatActivity() {

    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnBack: Button
    private lateinit var btnVoiceInput: Button
    private var number: String = ""

    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                etMessage.setText(spokenText)
            } else {
                Toast.makeText(this, "Nie rozpoznano tekstu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms)

        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        btnVoiceInput = findViewById(R.id.btnVoiceInput)

        number = intent.getStringExtra("NUMBER") ?: ""

        btnVoiceInput.setOnClickListener {
            startVoiceInput()
        }

        btnSend.setOnClickListener {
            val msg = etMessage.text.toString().trim()
            if (msg.isEmpty()) {
                Toast.makeText(this, "Wpisz wiadomość", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (number.isEmpty()) {
                Toast.makeText(this, "Nie wybrano numeru", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sprawdź, czy mamy uprawnienia
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                // Mamy uprawnienia, wyślij
                sendSms(number, msg)
            } else {
                Toast.makeText(this, "Brak uprawnień do wysyłania SMS", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            @Suppress("DEPRECATION") // Używamy starej metody dla zgodności z Android 11
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)

            Toast.makeText(this, "Wiadomość wysłana!", Toast.LENGTH_SHORT).show()
            etMessage.text.clear() // Wyczyść pole po wysłaniu
            finish()

        } catch (e: Exception) {
            Toast.makeText(this, "Błąd wysyłania SMS: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Powiedz treść wiadomości")
        }
        voiceLauncher.launch(intent)
    }
}