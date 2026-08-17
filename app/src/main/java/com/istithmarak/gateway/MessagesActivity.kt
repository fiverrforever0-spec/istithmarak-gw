package com.istithmarak.gateway

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONArray

class MessagesActivity : AppCompatActivity() {

    private lateinit var phoneInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var bulkNumbersInput: EditText
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
        }

        val title = TextView(this).apply {
            text = "الرسائل"
            textSize = 22f
        }

        phoneInput = EditText(this).apply { hint = "رقم واحد (096xxxxxxx)" }
        messageInput = EditText(this).apply { hint = "نص الرسالة" }

        val btnSendSingle = Button(this).apply {
            text = "إرسال لرقم واحد"
            setOnClickListener { sendSingle() }
        }

        bulkNumbersInput = EditText(this).apply {
            hint = "أرقام متعددة (كل رقم في سطر)"
            minLines = 3
        }

        val btnSendBulk = Button(this).apply {
            text = "إرسال جماعي (حد أقصى 30)"
            setOnClickListener { sendBulk() }
        }

        logView = TextView(this).apply {
            setPadding(0, 20, 0, 0)
            textSize = 14f
        }

        layout.addView(title)
        layout.addView(phoneInput)
        layout.addView(messageInput)
        layout.addView(btnSendSingle)
        layout.addView(bulkNumbersInput)
        layout.addView(btnSendBulk)
        layout.addView(logView)
        setContentView(layout)
    }

    private fun sendSingle() {
        val phone = phoneInput.text.toString().trim()
        val message = messageInput.text.toString().trim()
        if (phone.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "أدخل الرقم والرسالة", Toast.LENGTH_SHORT).show()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val success = ApiClient.sendMessage(this@MessagesActivity, phone, message, "instant")
            withContext(Dispatchers.Main) {
                logView.text = if (success) "✅ تم الإرسال إلى $phone" else "❌ فشل الإرسال"
            }
        }
    }

    private fun sendBulk() {
        val numbersText = bulkNumbersInput.text.toString().trim()
        val message = messageInput.text.toString().trim()
        if (numbersText.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "أدخل الأرقام والرسالة", Toast.LENGTH_SHORT).show()
            return
        }
        val phones = numbersText.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (phones.size > 30) {
            Toast.makeText(this, "الحد الأقصى 30 رقمًا", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            var successCount = 0
            for (phone in phones) {
                val ok = ApiClient.sendMessage(this@MessagesActivity, phone, message, "instant")
                if (ok) successCount++
            }
            withContext(Dispatchers.Main) {
                logView.text = "تم إرسال $successCount من ${phones.size}"
            }
        }
    }
}
