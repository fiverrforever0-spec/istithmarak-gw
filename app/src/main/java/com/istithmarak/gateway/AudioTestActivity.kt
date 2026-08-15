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
            text = "اضغط لبدء اختبار التقاط صوت المكالمة"
            textSize = 16f
            setPadding(0, 30, 0, 30)
        }

        val btnTest = Button(this).apply {
            text = "بدء مكالمة واختبار VOICE_CALL"
            setOnClickListener { startTest() }
        }

        layout.addView(statusText)
        layout.addView(btnTest)
        setContentView(layout)
    }

    private fun startTest() {
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
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:1234567890"))
            startActivity(intent)
        } catch (e: Exception) {
            statusText.text = "فشل بدء المكالمة: ${e.message}"
            return
        }

        statusText.text = "المكالمة بدأت، سيبدأ التسجيل بعد 5 ثوانٍ..."
        Thread {
            try {
                Thread.sleep(5000)
                runOnUiThread {
                    statusText.text = "بدأ التسجيل من VOICE_CALL لمدة 5 ثوانٍ..."
                }
                startRecording()
                Thread.sleep(5000)
                stopRecording()
                runOnUiThread {
                    statusText.text = "انتهى الاختبار. راجع السجل."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "خطأ أثناء الاختبار: ${e.message}"
                }
            }
        }.start()
    }

    private fun startRecording() {
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
                runOnUiThread { statusText.text = "فشل تهيئة AudioRecord" }
                return
            }
            audioRecord?.startRecording()
            isRecording = true

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
            runOnUiThread {
                statusText.text = "فشل بدء التسجيل: ${e.message}"
            }
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
    }
}
