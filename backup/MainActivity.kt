package com.istithmarak.gateway

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private var mqttClient: MqttClient? = null
    private val isRunning = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
        }

        statusText = TextView(this).apply {
            text = "جاهز للاتصال"
            textSize = 16f
            setPadding(0, 30, 0, 30)
        }

        val btnConnect = Button(this).apply {
            text = "تشغيل MQTT والاتصال"
            setOnClickListener { startMqttConnection() }
        }

        val btnDialTest = Button(this).apply {
            text = "اختبار أمر dial"
            setOnClickListener { testDialFromServer() }
        }

        layout.addView(statusText)
        layout.addView(btnConnect)
        layout.addView(btnDialTest)
        setContentView(layout)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val perms = arrayOf(
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE
            )
            if (perms.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
                ActivityCompat.requestPermissions(this, perms, 100)
            }
        }
    }

    private fun startMqttConnection() {
        if (isRunning.get()) {
            statusText.text = "الاتصال يعمل بالفعل"
            return
        }
        statusText.text = "جارٍ الاتصال بـ 127.0.0.1:1883 ..."
        isRunning.set(true)

        Thread {
            try {
                val brokerUrl = "tcp://127.0.0.1:1883"
                val clientId = "mobile_gateway_01"
                val client = MqttClient(brokerUrl, clientId, MemoryPersistence())
                val options = MqttConnectOptions().apply {
                    isAutomaticReconnect = true
                    isCleanSession = true
                    connectionTimeout = 10
                    keepAliveInterval = 20
                }

                client.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        runOnUiThread { statusText.text = "✅ متصل بالخادم: $serverURI" }
                        try {
                            client.subscribe("node/command/mobile_gateway_01", 1)
                            runOnUiThread { statusText.append("\nتم الاشتراك في node/command/mobile_gateway_01") }
                            sendResponse("status", "success", "Mobile gateway connected")
                        } catch (e: Exception) {
                            runOnUiThread { statusText.text = "❌ خطأ في الاشتراك: ${e.message}" }
                        }
                    }

                    override fun connectionLost(cause: Throwable?) {
                        runOnUiThread { statusText.text = "انقطع الاتصال: ${cause?.message}" }
                        isRunning.set(false)
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val payload = message?.payload?.let { String(it) } ?: ""
                        runOnUiThread { statusText.text = "📩 استقبال أمر: $payload" }
                        handleCommand(payload)
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                client.connect(options)
                mqttClient = client
            } catch (e: Exception) {
                runOnUiThread { statusText.text = "❌ فشل الاتصال: ${e.message}" }
                isRunning.set(false)
            }
        }.start()
    }

    private fun handleCommand(payload: String) {
        try {
            val json = JSONObject(payload)
            val command = json.optString("command", "")
            if (command == "dial") {
                val phone = json.optJSONObject("payload")?.optString("phone", "") ?: ""
                if (phone.isNotEmpty()) {
                    runOnUiThread { statusText.text = "📞 جارٍ الاتصال بالرقم: $phone" }
                    placeCall(phone)
                    sendResponse("dial", "success", "تم الاتصال بـ $phone")
                } else {
                    sendResponse("dial", "error", "رقم مفقود")
                }
            }
        } catch (e: Exception) {
            sendResponse("unknown", "error", e.message ?: "")
        }
    }

    private fun placeCall(phone: String) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone"))
                startActivity(intent)
            } else {
                statusText.text = "❌ صلاحية CALL_PHONE غير ممنوحة"
                sendResponse("dial", "error", "صلاحية المكالمة غير ممنوحة")
            }
        } catch (e: Exception) {
            sendResponse("dial", "error", e.message ?: "")
        }
    }

    private fun sendResponse(command: String, status: String, message: String) {
        try {
            val json = JSONObject().apply {
                put("command", command)
                put("status", status)
                put("message", message)
                put("timestamp", System.currentTimeMillis())
            }
            mqttClient?.publish("node/response/mobile_gateway_01", json.toString().toByteArray(), 1, false)
        } catch (_: Exception) {}
    }

    private fun testDialFromServer() {
        sendResponse("dial", "test", "تم إرسال أمر تجريبي")
        Toast.makeText(this, "أرسل أمر dial من الخادم وشاهد النتيجة", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.set(false)
        try {
            mqttClient?.disconnect()
            mqttClient?.close()
        } catch (_: Exception) {}
    }
}
