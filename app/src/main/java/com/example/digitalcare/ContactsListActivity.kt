package com.example.digitalcare

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import java.util.*

class ContactsListActivity : AppCompatActivity() {

    private lateinit var lvContacts: ListView
    private lateinit var btnBack: Button
    private lateinit var btnAdd: Button
    private lateinit var btnVoice: Button
    private lateinit var tvTitle: TextView
    private lateinit var tts: TextToSpeech

    private lateinit var adapter: ArrayAdapter<String>
    private val contactsList = ArrayList<String>()
    private var mode = "VIEW"

    private val voiceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                matches?.firstOrNull()?.let { handleVoiceCommand(it) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts_list)

        mode = intent.getStringExtra("MODE") ?: "VIEW"

        lvContacts = findViewById(R.id.lvContacts)
        btnBack = findViewById(R.id.btnBack)
        btnAdd = findViewById(R.id.btnAdd)
        btnVoice = findViewById(R.id.btnVoice)
        tvTitle = findViewById(R.id.tvTitle)


        when (mode) {
            "CALL" -> tvTitle.text = "Wybierz Kontakt (Zadzwoń)"
            "SMS" -> tvTitle.text = "Wybierz Kontakt (SMS)"
            else -> tvTitle.text = "LISTA KONTAKTÓW"
        }


        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
            }
        }

        loadContacts()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, contactsList)
        lvContacts.adapter = adapter

        btnBack.setOnClickListener { finish() }
        btnAdd.setOnClickListener { showAddContactDialog() }
        btnVoice.setOnClickListener { startVoiceRecognition() }

        lvContacts.setOnItemClickListener { _, _, pos, _ ->
            if (pos >= contactsList.size) return@setOnItemClickListener
            val number = contactsList[pos].split(" - ")[1]
            when (mode) {
                "CALL" -> {
                    startActivity(Intent(this, CallActivity::class.java).putExtra("NUMBER", number))
                    finish()
                }
                "SMS" -> {
                    startActivity(Intent(this, SMSActivity::class.java).putExtra("NUMBER", number))
                    finish()
                }
                else -> showEditDeleteDialog(pos)
            }
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Powiedz imię kontaktu")
        }
        // Uruchomienie systemowego okna dialogowego z mikrofonem
        voiceLauncher.launch(intent) }

    private fun handleVoiceCommand(command: String) {
        val clean = command.lowercase().trim()
        val index = contactsList.indexOfFirst { it.lowercase().contains(clean) }

        if (index != -1) {
            val number = contactsList[index].split(" - ")[1]
            when (mode) {
                "CALL" -> startActivity(Intent(this, CallActivity::class.java).putExtra("NUMBER", number))
                "SMS" -> startActivity(Intent(this, SMSActivity::class.java).putExtra("NUMBER", number))
                else -> showEditDeleteDialog(index)
            }
        } else {
            Toast.makeText(this, "Nie znaleziono: $clean", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogName)
        val etNumber = dialogView.findViewById<EditText>(R.id.etDialogNumber)

        AlertDialog.Builder(this)
            .setTitle("Dodaj kontakt")
            .setView(dialogView)
            .setPositiveButton("Zapisz") { _, _ ->
                val name = etName.text.toString().trim()
                val number = etNumber.text.toString().trim()
                if (name.isNotEmpty() && number.isNotEmpty()) {
                    contactsList.add("$name - $number")
                    saveContacts()
                    adapter.notifyDataSetChanged()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun showEditDeleteDialog(pos: Int) {
        val contact = contactsList[pos]
        val parts = contact.split(" - ")
        if (parts.size < 2) return

        val name = parts[0]
        val number = parts[1]

        val options = arrayOf(
            "Edytuj",
            "Usuń",
            "USTAW JAKO KONTAKT ALARMOWY (SOS)"
        )

        AlertDialog.Builder(this)
            .setTitle(name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editContact(pos, name, number)
                    1 -> deleteContact(pos)
                    2 -> setSosContact(contact, name)
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun setSosContact(contactString: String, name: String) {
        getSharedPreferences("contacts", MODE_PRIVATE).edit {
            putString("sos_contact_string", contactString)
            apply()
        }
        Toast.makeText(this, "$name ustawiono jako kontakt alarmowy SOS", Toast.LENGTH_LONG).show()
    }

    private fun editContact(pos: Int, oldName: String, oldNumber: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogName)
        val etNumber = dialogView.findViewById<EditText>(R.id.etDialogNumber)
        etName.setText(oldName)
        etNumber.setText(oldNumber)

        AlertDialog.Builder(this)
            .setTitle("Edytuj kontakt")
            .setView(dialogView)
            .setPositiveButton("Zapisz") { _, _ ->
                val name = etName.text.toString().trim()
                val number = etNumber.text.toString().trim()
                if (name.isNotEmpty() && number.isNotEmpty()) {
                    contactsList[pos] = "$name - $number"
                    saveContacts()
                    adapter.notifyDataSetChanged()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun deleteContact(pos: Int) {
        AlertDialog.Builder(this)
            .setMessage("Czy usunąć kontakt ${contactsList[pos]}?")
            .setPositiveButton("Tak") { _, _ ->
                contactsList.removeAt(pos)
                saveContacts()
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Nie", null)
            .show()
    }

    private fun loadContacts() {
        val set = getSharedPreferences("contacts", MODE_PRIVATE)
            .getStringSet("contacts_set", setOf()) ?: return
        contactsList.clear()
        contactsList.addAll(set)
    }

    private fun saveContacts() {
        getSharedPreferences("contacts", MODE_PRIVATE).edit {
            putStringSet("contacts_set", contactsList.toSet())
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}