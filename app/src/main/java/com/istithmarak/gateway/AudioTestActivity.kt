package com.istithmarak.gateway

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
import java.io.File
import java.io.FileOutputStream

class AudioTestActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
        }

        statusText = TextView(this).apply {
            text = "خطوات الاختبار:\n1. اضغط بدء مكالمة اختبار.\n2. عد إلى هذا التطبيق.\n3. اضغط بدء تسجيل VOICE_CALL."
            textSize = 16f
            setPadding(0, 30, 0, 30)
        }

        val btnCall = Button(this).apply {
            text = "بدء مكالمة اختبار"
            setOnClickListener { startTestCall() }
        }

        val btnRecord = Button(this).apply {
            text = "بدء تسجيل VOICE_CALL"
            setOnClickListener { startVoiceCallRecording() }
        }

        val btnStopRecord = Button(this).apply {
            text = "إيقاف التسجيل"
            setOnClickListener { stopRecording() }
        }

        layout.addView(statusText)
        layout.addView(btnCall)
        layout.addView(btnRecord)
        layout.addView(btnStopRecord)
        setContentView(layout)
    }

    private fun startTestCall() {
        // طلب أذونات الاتصال والتسجيل إذا لزم
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 200)
            return
        }

        statusText.text = "جارٍ بدء المكالمة..."
        try {
            // استخدم رقمًا صالحًا للاختبار (مثلاً رقم فودافون أو أي رقم)
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:1234567890"))
            startActivity(intent)
        } catch (e: Exception) {
            statusText.text = "فشل بدء المكالمة: ${e.message}"
        }
    }

    private fun startVoiceCallRecording() {
        // تأكد من أن المكالمة جارية (لا نستطيع الجزم برمجياً، لكن المستخدم سيعرف)
        if (isRecording) {
            statusText.text = "التسجيل جارٍ بالفعل"
            return
        }

        val sampleRate = 8000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = minBuffer.coerceAtLeast(2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_CALL,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                statusText.text = "فشل تهيئة AudioRecord (الجهاز لا يدعم VOICE_CALL غالبًا)"
                return
            }
            audioRecord?.startRecording()
            isRecording = true
            statusText.text = "بدأ التسجيل من VOICE_CALL لمدة غير محددة..."

            val buffer = ByteArray(bufferSize)
            val pcmFile = File(externalCacheDir, "voice_call_test_${System.currentTimeMillis()}.pcm")
            val output = FileOutputStream(pcmFile)

            recordingThread = Thread {
                try {
                    while (isRecording) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            output.write(buffer, 0, read)
                        }
                    }
                    output.close()
                    runOnUiThread {
                        statusText.text = "تم حفظ عينة PCM في ${pcmFile.absolutePath}"
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        statusText.text = "خطأ في التسجيل: ${e.message}"
                    }
                    try { output.close() } catch (_: Exception) {}
                }
            }
            recordingThread?.start()
        } catch (e: Exception) {
            statusText.text = "فشل بدء التسجيل: ${e.message}"
            isRecording = false
        }
    }

    private fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioRecord = null
        recordingThread?.join(1000)
        statusText.text = "تم إيقاف التسجيل"
    }
}
