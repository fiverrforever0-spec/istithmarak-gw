package com.istithmarak.gateway

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONArray

class NumbersActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var phoneInput: EditText
    private lateinit var nameInput: EditText
    private val numbers = mutableListOf<String>()
    private val phones = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
        }

        val title = TextView(this).apply {
            text = "الأرقام"
            textSize = 22f
        }

        phoneInput = EditText(this).apply { hint = "رقم الهاتف" }
        nameInput = EditText(this).apply { hint = "الاسم (اختياري)" }

        val btnAdd = Button(this).apply {
            text = "إضافة"
            setOnClickListener { addNumber() }
        }

        listView = ListView(this)
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val phone = phones.getOrNull(position)
            if (phone != null) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("حذف")
                    .setMessage("حذف $phone ؟")
                    .setPositiveButton("حذف") { _, _ -> deleteNumber(phone) }
                    .setNegativeButton("إلغاء", null)
                    .show()
            }
            true
        }

        layout.addView(title)
        layout.addView(phoneInput)
        layout.addView(nameInput)
        layout.addView(btnAdd)
        layout.addView(listView)
        setContentView(layout)

        loadNumbers()
    }

    private fun loadNumbers() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val arr = ApiClient.getNumbers(this@NumbersActivity)
                numbers.clear()
                phones.clear()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val phone = obj.optString("phone", "")
                    val name = obj.optString("name", "")
                    phones.add(phone)
                    numbers.add(if (name.isEmpty()) phone else "$phone ($name)")
                }
                withContext(Dispatchers.Main) {
                    val adapter = ArrayAdapter(this@NumbersActivity, android.R.layout.simple_list_item_1, numbers)
                    listView.adapter = adapter
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NumbersActivity, "فشل جلب الأرقام: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun addNumber() {
        val phone = phoneInput.text.toString().trim()
        val name = nameInput.text.toString().trim()
        if (phone.isEmpty()) {
            Toast.makeText(this, "أدخل رقم الهاتف", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val success = ApiClient.addNumber(this@NumbersActivity, phone, name)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@NumbersActivity, if (success) "تمت الإضافة" else "فشل", Toast.LENGTH_SHORT).show()
                phoneInput.text.clear()
                nameInput.text.clear()
                loadNumbers()
            }
        }
    }

    private fun deleteNumber(phone: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val success = ApiClient.deleteNumber(this@NumbersActivity, phone)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@NumbersActivity, if (success) "تم الحذف" else "فشل", Toast.LENGTH_SHORT).show()
                loadNumbers()
            }
        }
    }
}
