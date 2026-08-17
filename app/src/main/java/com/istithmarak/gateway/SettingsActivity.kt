package com.istithmarak.gateway

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
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
        val info = TextView(this).apply {
            text = "قريباً: عنوان السيرفر ونصوص الرسائل"
            setPadding(0, 20, 0, 0)
        }
        layout.addView(title)
        layout.addView(info)
        setContentView(layout)
    }
}
