package com.istithmarak.gateway

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
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
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private var mqttClient: MqttClient? = null
    private val isRunning = AtomicBoolean(false)
    private var tts: TextToSpeech? = null

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

        val btnSendSms = Button(this).apply {
            text = "إرسال SMS ترحيبي"
            setOnClickListener { sendWelcomeSms() }
        }

        val btnCallVoice = Button(this).apply {
            text = "اتصال صوتي ترحيبي"
            setOnClickListener { startWelcomeCall() }
        }

        val btnDialTest = Button(this).apply {
            text = "اختبار أمر dial"
            setOnClickListener { testDialFromServer() }
        }

        val btnAudioTest = Button(this).apply {
            text = "اختبار توافق الصوت"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, AudioTestActivity::class.java))
            }
        }

        layout.addView(statusText)
        layout.addView(btnConnect)
        layout.addView(btnSendSms)
        layout.addView(btnCallVoice)
        layout.addView(btnDialTest)
        layout.addView(btnAudioTest)
        setContentView(layout)

        // طلب أذونات المكالمات والرسائل
        requestInitialPermissions()

        // تهيئة TextToSpeech
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    private fun requestInitialPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val perms = arrayOf(
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.SEND_SMS
            )
            val missing = perms.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
            }
        }
    }

    private fun sendWelcomeSms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "صلاحية إرسال الرسائل غير ممنوحة", Toast.LENGTH_SHORT).show()
            return
        }
        val phone = "0920743054"
        val message = "مرحب بيك في استثمارك للحلول الاستثمارية الذكية"
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phone, null, message, null, null)
            statusText.text = "تم إرسال SMS إلى $phone"
        } catch (e: Exception) {
            statusText.text = "فشل إرسال SMS: ${e.message}"
        }
    }

    private fun startWelcomeCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "صلاحية الاتصال غير ممنوحة", Toast.LENGTH_SHORT).show()
            return
        }
        val phone = "0920743054"
        try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone"))
            startActivity(intent)
            statusText.text = "بدأ الاتصال بـ $phone. سيتم تشغيل الرسالة بعد 10 ثوانٍ..."

            // بعد 10 ثوانٍ، شغّل الرسالة الصوتية
            Handler(Looper.getMainLooper()).postDelayed({
                speakWelcomeMessage()
            }, 10000)
        } catch (e: Exception) {
            statusText.text = "فشل بدء المكالمة: ${e.message}"
        }
    }

    private fun speakWelcomeMessage() {
        val message = "للتواصل مع استثمارك أرسل على واتساب 0962411479"
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "welcome_msg")
            statusText.text = "تم تشغيل الرسالة الصوتية"
        } catch (e: Exception) {
            statusText.text = "فشل تشغيل الصوت: ${e.message}"
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
                    runOnUiThread {
                        statusText.text = "📞 جارٍ الاتصال بالرقم: $phone"
                        try {
                            placeCall(phone)
                            sendResponse("dial", "success", "تم الاتصال بـ $phone")
                        } catch (e: Exception) {
                            statusText.text = "❌ خطأ في الاتصال: ${e.message}"
                            sendResponse("dial", "error", e.message ?: "")
                        }
                    }
                } else {
                    sendResponse("dial", "error", "رقم مفقود")
                }
            }
        } catch (e: Exception) {
            sendResponse("unknown", "error", e.message ?: "")
        }
    }

    private fun placeCall(phone: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 100)
            statusText.text = "منح إذن المكالمات مطلوب"
            throw SecurityException("CALL_PHONE permission not granted")
        }
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
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
        tts?.stop()
        tts?.shutdown()
    }
}
