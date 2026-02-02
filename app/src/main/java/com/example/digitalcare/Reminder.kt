package com.example.digitalcare

data class Reminder(
    val id: Int,
    val name: String,
    val hour: Int,
    val minute: Int,
    val days: List<String>
) {
    // Zwraca czytelny tekst dla listy
    fun getDisplayText(): String {
        val time = String.format("%02d:%02d", hour, minute)

        val daysText = if (days.size == 7) {
            "Codziennie" // Wszystkie 7 zaznaczone
        } else if (days.isEmpty()) {
            "Alarm jednorazowy" // Zero zaznaczonych
        } else {
            days.joinToString(", ") // Kilka zaznaczonych
        }

        return "$name - $time ($daysText)"
    }
}