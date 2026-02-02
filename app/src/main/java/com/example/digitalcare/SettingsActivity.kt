package com.example.digitalcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnGoToContacts: Button = findViewById(R.id.btnGoToContacts)
        val btnGoToReminders: Button = findViewById(R.id.btnGoToReminders)
        val btnBack: Button = findViewById(R.id.btnBack)

        // Przejdź do listy kontaktów
        btnGoToContacts.setOnClickListener {
            startActivity(Intent(this, ContactsListActivity::class.java).apply {
                putExtra("MODE", "VIEW")
            })
        }

        // Przejdź do przypomnień
        btnGoToReminders.setOnClickListener {
            startActivity(Intent(this, RemindersListActivity::class.java))
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}