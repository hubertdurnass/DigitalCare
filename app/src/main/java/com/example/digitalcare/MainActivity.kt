package com.example.digitalcare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSOS: Button = findViewById(R.id.btnSOS)
        val btnCall: Button = findViewById(R.id.btnCall)
        val btnSMS: Button = findViewById(R.id.btnSMS)
        val btnSettings: Button = findViewById(R.id.btnSettings)

        btnSOS.setOnClickListener {
            startActivity(Intent(this, SOSActivity::class.java))
        }
        btnCall.setOnClickListener {
            startActivity(Intent(this, CallListActivity::class.java))
        }
        btnSMS.setOnClickListener {
            startActivity(Intent(this, SMSListActivity::class.java))
        }
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        requestPhonePermissions()
        checkOverlayPermission()
    }

    private fun requestPhonePermissions() {
        val required = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.SEND_SMS,
            Manifest.permission.USE_EXACT_ALARM,
            Manifest.permission.SCHEDULE_EXACT_ALARM
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(
                this,
                "Wymagane uprawnienie: Wyświetlanie nad innymi aplikacjami",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}