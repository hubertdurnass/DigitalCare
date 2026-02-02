package com.example.digitalcare

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import kotlin.collections.ArrayList

class RemindersListActivity : AppCompatActivity() {

    private lateinit var lvReminders: ListView
    private lateinit var btnAddReminder: Button
    private lateinit var btnBack: Button

    private lateinit var adapter: ArrayAdapter<String>
    private val remindersList = ArrayList<Reminder>()
    private val remindersDisplayList = ArrayList<String>()

    private var selectedHour = -1
    private var selectedMinute = -1
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders_list)

        lvReminders = findViewById(R.id.lvReminders)
        btnAddReminder = findViewById(R.id.btnAddReminder)
        btnBack = findViewById(R.id.btnBack)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, remindersDisplayList)
        lvReminders.adapter = adapter

        btnBack.setOnClickListener { finish() }

        btnAddReminder.setOnClickListener {
            showAddReminderDialog(null)
        }

        lvReminders.setOnItemClickListener { _, _, pos, _ ->
            showEditDeleteDialog(pos)
        }

        loadReminders()
    }

    private fun showEditDeleteDialog(pos: Int) {
        val reminder = remindersList[pos]
        val options = arrayOf("Edytuj", "Usuń")

        AlertDialog.Builder(this)
            .setTitle(reminder.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddReminderDialog(reminder) // Otwórz w trybie edycji
                    1 -> deleteReminder(reminder)
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun deleteReminder(reminder: Reminder) {
        // Anuluj systemowy alarm
        AlarmScheduler.cancel(this, reminder)

        // Usuń z listy
        remindersList.remove(reminder)

        // Zapisz i odśwież
        saveAndRefreshLists()
        Toast.makeText(this, "Przypomnienie usunięte", Toast.LENGTH_SHORT).show()
    }

    private fun showAddReminderDialog(reminderToEdit: Reminder?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder, null)
        val etReminderName = dialogView.findViewById<EditText>(R.id.etReminderName)
        val btnSetTime = dialogView.findViewById<Button>(R.id.btnSetTime)

        // Checkboxy
        val cbMon = dialogView.findViewById<CheckBox>(R.id.cbMon)
        val cbTue = dialogView.findViewById<CheckBox>(R.id.cbTue)
        val cbWed = dialogView.findViewById<CheckBox>(R.id.cbWed)
        val cbThu = dialogView.findViewById<CheckBox>(R.id.cbThu)
        val cbFri = dialogView.findViewById<CheckBox>(R.id.cbFri)
        val cbSat = dialogView.findViewById<CheckBox>(R.id.cbSat)
        val cbSun = dialogView.findViewById<CheckBox>(R.id.cbSun)
        val weekdaysMap = mapOf(
            "Pon" to cbMon, "Wt" to cbTue, "Śr" to cbWed, "Czw" to cbThu,
            "Pt" to cbFri, "Sob" to cbSat, "Niedz" to cbSun
        )

        // Reset
        selectedHour = -1
        selectedMinute = -1

        // Tryb edycji
        if (reminderToEdit != null) {
            etReminderName.setText(reminderToEdit.name)
            selectedHour = reminderToEdit.hour
            selectedMinute = reminderToEdit.minute
            btnSetTime.text = String.format(Locale.getDefault(), "GODZINA: %02d:%02d", selectedHour, selectedMinute)

            for (day in reminderToEdit.days) {
                weekdaysMap[day]?.isChecked = true
            }
        }

        btnSetTime.setOnClickListener {
            val cal = Calendar.getInstance()
            val initialHour = if (selectedHour != -1) selectedHour else cal.get(Calendar.HOUR_OF_DAY)
            val initialMinute = if (selectedMinute != -1) selectedMinute else cal.get(Calendar.MINUTE)

            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute
                    btnSetTime.text = String.format(Locale.getDefault(), "GODZINA: %02d:%02d", hourOfDay, minute)
                },
                initialHour,
                initialMinute,
                true
            ).show()
        }

        AlertDialog.Builder(this)
            .setTitle(if (reminderToEdit != null) "Edytuj Przypomnienie" else "Nowe Przypomnienie")
            .setView(dialogView)
            .setPositiveButton("Zapisz") { _, _ ->
                val name = etReminderName.text.toString()

                if (name.isBlank() || selectedHour == -1) {
                    Toast.makeText(this, "Wpisz nazwę i wybierz godzinę", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // (Jeśli edytujemy) Anuluj stary alarm
                if (reminderToEdit != null) {
                    AlarmScheduler.cancel(this, reminderToEdit)
                    remindersList.remove(reminderToEdit) // Usuń starą wersję z listy
                }

                // Zbieranie dni tygodnia
                val selectedDays = mutableListOf<String>()
                weekdaysMap.forEach { (dayName, checkBox) ->
                    if (checkBox.isChecked) {
                        selectedDays.add(dayName)
                    }
                }

                val newReminder = Reminder(
                    id = reminderToEdit?.id ?: System.currentTimeMillis().toInt(),
                    name = name,
                    hour = selectedHour,
                    minute = selectedMinute,
                    days = selectedDays
                )

                // Ustaw nowy alarm
                AlarmScheduler.schedule(this, newReminder)

                // Dodaj do listy, zapisz i odśwież
                remindersList.add(newReminder)
                saveAndRefreshLists()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun saveAndRefreshLists() {
        // Sortuj listę
        remindersList.sortBy { it.hour * 60 + it.minute }

        // Zapisz prawdziwe dane
        saveReminders(remindersList)

        // Zaktualizuj listę wyświetlaną
        remindersDisplayList.clear()
        remindersDisplayList.addAll(remindersList.map { it.getDisplayText() })

        // Odśwież adapter
        adapter.notifyDataSetChanged()
    }

    private fun loadReminders() {
        val prefs = getSharedPreferences("reminders_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("reminders_json", null)

        if (json != null) {
            val type = object : TypeToken<ArrayList<Reminder>>() {}.type
            val loadedReminders: ArrayList<Reminder> = gson.fromJson(json, type)

            remindersList.clear()
            remindersList.addAll(loadedReminders)

            remindersDisplayList.clear()
            remindersDisplayList.addAll(remindersList.map { it.getDisplayText() })

            adapter.notifyDataSetChanged()
        }
    }

    private fun saveReminders(list: ArrayList<Reminder>) {
        val prefs = getSharedPreferences("reminders_prefs", Context.MODE_PRIVATE)
        val json = gson.toJson(list)
        // Użycie rozszerzenia KTX (funkcja edit) automatycznie wykonuje 'apply()' zapewniając zapis w tle
        prefs.edit {
            putString("reminders_json", json)
        }
    }
}