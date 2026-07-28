package com.istithmarak.gateway

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions

class MqttControlService : Service() {

    companion object {
        private const val TAG = "MqttControlService"
        private const val CHANNEL_ID = "gateway_control_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mqttClient: MqttClient? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service starting...")
        startForeground(NOTIFICATION_ID, buildNotification())
        serviceScope.launch { initializeMqtt() }
        return START_STICKY
    }

    private fun initializeMqtt() {
        try {
            val brokerUrl = "tcp://your-mqtt-broker.com:1883" // عدّل هذا
            val clientId = "gateway-${System.currentTimeMillis()}"

            mqttClient = MqttClient(brokerUrl, clientId, null)
            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 20
            }

            mqttClient?.connect(options)
            Log.i(TAG, "MQTT connected to $brokerUrl")

            mqttClient?.subscribe("istithmarak/gateway/commands") { topic, message ->
                val payload = String(message.payload)
                Log.i(TAG, "Command received on $topic: $payload")
                handleCommand(payload)
            }
        } catch (e: Exception) {
            Log.e(TAG, "MQTT failed: ${e.message}", e)
        }
    }

    private fun handleCommand(command: String) {
        when (command.uppercase()) {
            "STATUS" -> {
                val status = "Call=${GsmInCallService.activeCall != null}, SIP=${SipClientManager.isReady()}"
                Log.i(TAG, "Status: $status")
            }
            "RESTART" -> {
                Log.i(TAG, "Restart command received")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gateway Control",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background control for GSM Gateway"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Istithmarak Gateway")
            .setContentText("خدمة التحكم عن بعد نشطة")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            mqttClient?.disconnect()
            mqttClient?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
        Log.i(TAG, "Service destroyed")
    }
}
