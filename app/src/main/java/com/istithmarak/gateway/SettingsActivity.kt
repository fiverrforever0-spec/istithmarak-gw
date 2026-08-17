package com.istithmarak.gateway

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var serverUrlInput: EditText
    private lateinit var instantMsgInput: EditText
    private lateinit var reminderMsgInput: EditText
    private lateinit var delayInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
        }

        val title = TextView(this).apply {
            text = "الإعدادات"
            textSize = 22f
        }

        serverUrlInput = EditText(this).apply {
            hint = "عنوان السيرفر"
            setText(ApiClient.getServerUrl(this@SettingsActivity))
        }

        instantMsgInput = EditText(this).apply {
            hint = "الرسالة الفورية"
        }

        reminderMsgInput = EditText(this).apply {
            hint = "رسالة التذكير"
        }

        delayInput = EditText(this).apply {
            hint = "مدة التذكير (ساعات)"
            setText("24")
        }

        val btnSave = Button(this).apply {
            text = "حفظ الإعدادات"
            setOnClickListener { saveSettings() }
        }

        layout.addView(title)
        layout.addView(serverUrlInput)
        layout.addView(instantMsgInput)
        layout.addView(reminderMsgInput)
        layout.addView(delayInput)
        layout.addView(btnSave)
        setContentView(layout)
    }

    private fun saveSettings() {
        val url = serverUrlInput.text.toString().trim()
        if (url.isNotEmpty()) {
            ApiClient.saveServerUrl(this, url)
        }

        val instant = instantMsgInput.text.toString().trim()
        val reminder = reminderMsgInput.text.toString().trim()
        val delay = delayInput.text.toString().trim()

        // حفظ محلي (يمكن رفعه للسيرفر لاحقًا)
        val prefs = getSharedPreferences("gateway_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("instant_message", instant)
            .putString("reminder_message", reminder)
            .putString("reminder_delay", delay)
            .apply()

        Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
    }
}
