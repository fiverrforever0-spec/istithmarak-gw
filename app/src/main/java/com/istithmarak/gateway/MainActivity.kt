package com.istithmarak.gateway

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var summaryText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
        }

        val title = TextView(this).apply {
            text = "لوحة تحكم استثمارك"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        statusText = TextView(this).apply {
            text = "حالة الخادم: جارٍ الفحص..."
            textSize = 16f
            setPadding(0, 20, 0, 10)
        }

        summaryText = TextView(this).apply {
            text = "الأرقام: - | الرسائل: -"
            textSize = 16f
            setPadding(0, 0, 0, 30)
        }

        val btnNumbers = Button(this).apply {
            text = "📋 الأرقام"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, NumbersActivity::class.java))
            }
        }

        val btnMessages = Button(this).apply {
            text = "💬 الرسائل"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, MessagesActivity::class.java))
            }
        }

        val btnReports = Button(this).apply {
            text = "📊 التقارير"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ReportsActivity::class.java))
            }
        }

        val btnSettings = Button(this).apply {
            text = "⚙️ الإعدادات"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(summaryText)
        layout.addView(btnNumbers)
        layout.addView(btnMessages)
        layout.addView(btnReports)
        layout.addView(btnSettings)
        setContentView(layout)

        refreshDashboard()
    }

    private fun refreshDashboard() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val numbers = ApiClient.getNumbers(this@MainActivity)
                val messages = ApiClient.getMessages(this@MainActivity)
                withContext(Dispatchers.Main) {
                    statusText.text = "حالة الخادم: ✅ متصل"
                    summaryText.text = "الأرقام: ${numbers.length()} | الرسائل: ${messages.length()}"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "حالة الخادم: ❌ غير متصل"
                    summaryText.text = "تأكد من تشغيل السيرفر"
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
    }
}
