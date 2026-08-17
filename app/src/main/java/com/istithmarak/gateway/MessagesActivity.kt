package com.istithmarak.gateway

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MessagesActivity : AppCompatActivity() {
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
        val info = TextView(this).apply {
            text = "قريباً: إرسال فردي وجماعي وسجل"
            setPadding(0, 20, 0, 0)
        }
        layout.addView(title)
        layout.addView(info)
        setContentView(layout)
    }
}
