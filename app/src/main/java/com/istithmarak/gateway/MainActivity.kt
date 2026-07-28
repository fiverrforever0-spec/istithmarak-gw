package com.istithmarak.gateway

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.ANSWER_PHONE_CALLS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            checkAndRequestDefaultDialer()
        } else {
            Toast.makeText(this, "الأذونات مطلوبة لتشغيل البوابة", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
        }

        val title = TextView(this).apply {
            text = "Istithmarak GSM Gateway"
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val statusText = TextView(this).apply {
            text = "النظام جاهز. يرجى منح الأذونات وتعيين التطبيق كبرنامج اتصال افتراضي."
            textSize = 16f
            setPadding(0, 30, 0, 50)
        }

        val btnPermissions = Button(this).apply {
            text = "منح الأذونات"
            setOnClickListener { requestRuntimePermissions() }
        }

        val btnDefaultDialer = Button(this).apply {
            text = "تعيين كبرنامج اتصال افتراضي"
            setOnClickListener { requestDefaultDialer() }
        }

        val btnStartMqtt = Button(this).apply {
            text = "تشغيل خدمة التحكم عن بعد"
            setOnClickListener {
                startService(Intent(this@MainActivity, MqttControlService::class.java))
                Toast.makeText(this@MainActivity, "تم تشغيل خدمة MQTT", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(btnPermissions)
        layout.addView(btnDefaultDialer)
        layout.addView(btnStartMqtt)
        setContentView(layout)

        if (!hasAllPermissions()) {
            requestRuntimePermissions()
        } else {
            checkAndRequestDefaultDialer()
        }
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestRuntimePermissions() {
        permissionLauncher.launch(requiredPermissions)
    }

    private fun checkAndRequestDefaultDialer() {
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        if (packageName != telecomManager.defaultDialerPackage) {
            requestDefaultDialer()
        }
    }

    private fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
            ) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                startActivityForResult(intent, 101)
            }
        } else {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            }
            startActivity(intent)
        }
    }
}
