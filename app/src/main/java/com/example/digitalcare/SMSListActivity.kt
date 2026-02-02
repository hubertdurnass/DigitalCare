package com.example.digitalcare

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class SMSListActivity : AppCompatActivity() {

    private lateinit var lvContacts: ListView
    private lateinit var btnScrollUp: ImageButton
    private lateinit var btnScrollDown: ImageButton
    private val contactsList = ArrayList<String>()
    private val namesList = ArrayList<String>()

    private val voiceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                matches?.firstOrNull()?.let { command ->
                    handleVoiceCommand(command)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_list)

        lvContacts = findViewById(R.id.lvContacts)
        btnScrollUp = findViewById(R.id.btnScrollUp)
        btnScrollDown = findViewById(R.id.btnScrollDown)

        val btnBack: Button = findViewById(R.id.btnBack)
        val btnVoice: Button = findViewById(R.id.btnVoice)

        btnBack.setOnClickListener { finish() }
        btnVoice.setOnClickListener { startVoiceRecognition() }

        // Wczytaj kontakty
        loadContacts()

        val adapter = ArrayAdapter(this, R.layout.list_item_contact, R.id.tvContactName, namesList)
        lvContacts.adapter = adapter

        // Kliknięcie na kontakt otwiera SMSActivity
        lvContacts.setOnItemClickListener { _, _, pos, _ ->
            val number = contactsList[pos].split(" - ")[1]
            startActivity(Intent(this, SMSActivity::class.java).putExtra("NUMBER", number))
            finish()
        }

        btnScrollUp.setOnClickListener {
            val firstVisible = lvContacts.firstVisiblePosition
            lvContacts.setSelection((firstVisible - 3).coerceAtLeast(0))
        }
        btnScrollDown.setOnClickListener {
            val firstVisible = lvContacts.firstVisiblePosition
            val total = lvContacts.adapter?.count ?: 0
            lvContacts.setSelection((firstVisible + 3).coerceAtMost(total - 1))
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Powiedz imię kontaktu")
        }
        voiceLauncher.launch(intent)
    }

    // Szuka kontaktu, do którego chcemy wysłać SMS
    private fun handleVoiceCommand(command: String) {
        val clean = command.lowercase().trim()
        val index = namesList.indexOfFirst { it.lowercase().contains(clean) }
        if (index != -1) {
            val number = contactsList[index].split(" - ")[1]
            startActivity(Intent(this, SMSActivity::class.java).putExtra("NUMBER", number))
            finish()
        } else {
            Toast.makeText(this, "Nie znaleziono: $clean", Toast.LENGTH_SHORT).show()
        }
    }

    // Wczytuje kontakty z SharedPreferences
    private fun loadContacts() {
        val set = getSharedPreferences("contacts", MODE_PRIVATE)
            .getStringSet("contacts_set", setOf()) ?: return
        contactsList.clear()
        namesList.clear()
        set.forEach { contact ->
            val parts = contact.split(" - ")
            if (parts.size == 2) {
                contactsList.add(contact)
                namesList.add(parts[0])
            }
        }
    }
}