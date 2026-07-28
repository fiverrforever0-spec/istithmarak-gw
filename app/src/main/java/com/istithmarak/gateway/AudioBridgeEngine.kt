package com.istithmarak.gateway

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*

object AudioBridgeEngine {

    private const val SAMPLE_RATE = 8000
    private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
    private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private const val TAG = "AudioBridgeEngine"

    private var isBridging = false
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var bridgeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startBridge() {
        if (isBridging) {
            Log.w(TAG, "Bridge already active")
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
        if (bufferSize <= 0) {
            Log.e(TAG, "Invalid buffer size: $bufferSize")
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                bufferSize
            ).apply {
                if (state != AudioRecord.STATE_INITIALIZED) {
                    throw IllegalStateException("AudioRecord init failed")
                }
            }

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AUDIO_FORMAT)
                        .setChannelMask(CHANNEL_CONFIG_OUT)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioRecord?.startRecording()
            audioTrack?.play()
            isBridging = true

            bridgeJob = scope.launch {
                val buffer = ByteArray(bufferSize)
                while (isActive && isBridging) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        // TODO: Production: send buffer to SIP/RTP encoder here
                        // Current: local loopback for pipeline verification
                        audioTrack?.write(buffer, 0, readSize)
                    }
                }
            }

            Log.i(TAG, "Audio bridge started")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start bridge: ${e.message}", e)
            stopBridge()
        }
    }

    fun stopBridge() {
        isBridging = false
        bridgeJob?.cancel()
        bridgeJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null

            Log.i(TAG, "Audio bridge stopped safely")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping bridge: ${e.message}", e)
        }
    }
}
