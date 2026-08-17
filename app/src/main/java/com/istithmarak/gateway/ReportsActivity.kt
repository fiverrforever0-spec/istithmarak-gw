package com.istithmarak.gateway

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class ReportsActivity : AppCompatActivity() {

    private lateinit var reportText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
        }

        val title = TextView(this).apply {
            text = "التقارير"
            textSize = 22f
        }

        reportText = TextView(this).apply {
            setPadding(0, 20, 0, 0)
            textSize = 16f
        }

        layout.addView(title)
        layout.addView(reportText)
        setContentView(layout)

        loadReport()
    }

    private fun loadReport() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val numbers = ApiClient.getNumbers(this@ReportsActivity)
                val messages = ApiClient.getMessages(this@ReportsActivity)
                val text = "📋 عدد الأرقام: ${numbers.length()}\n" +
                        "💬 عدد الرسائل: ${messages.length()}\n" +
                        "📅 آخر تحديث: ${System.currentTimeMillis()}"
                withContext(Dispatchers.Main) {
                    reportText.text = text
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    reportText.text = "فشل جلب التقارير: ${e.message}"
                }
            }
        }
    }
}
