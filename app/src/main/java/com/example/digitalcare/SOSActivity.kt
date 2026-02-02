package com.example.digitalcare

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SOSActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        triggerSosSequence()
    }

    private fun triggerSosSequence() {
        val prefs = getSharedPreferences("contacts", MODE_PRIVATE)
        val sosContactString = prefs.getString("sos_contact_string", null)

        // Sprawdzenie czy kontakt istnieje
        if (sosContactString.isNullOrBlank()) {
            Toast.makeText(this, "Najpierw ustaw kontakt SOS w Liście Kontaktów", Toast.LENGTH_LONG).show()
            finish() // Zamykamy aktywność i wracamy do MainActivity
            return
        }

        // Wyciągnięcie numeru
        val number = try {
            sosContactString.split(" - ")[1].trim()
        } catch (e: Exception) {
            Toast.makeText(this, "Błąd kontaktu SOS. Ustaw go ponownie.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Toast.makeText(this, "Alarm SOS! Dzwonienie...", Toast.LENGTH_SHORT).show()

        // Uruchomienie ekranu dzwonienia
        startDialing(number)
    }

    private fun startDialing(number: String) {
        val intent = Intent(this, DialingActivity::class.java).apply {
            putExtra("NUMBER", number)
            // Czyścimy flagi, żeby po rozłączeniu nie wracać tutaj
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish() // Natychmiast zamykamy SOSActivity, żeby zniknęła ze stosu
    }
}