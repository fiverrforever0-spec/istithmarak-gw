package com.istithmarak.gateway

import android.util.Log

object SipClientManager {

    private const val TAG = "SipClientManager"

    data class SipConfig(
        val serverIp: String,
        val extension: String,
        val secret: String,
        val port: Int = 5060,
        val useTls: Boolean = false
    )

    private var config: SipConfig? = null
    private var isRegistered = false
    private var isCallActive = false

    fun initialize(config: SipConfig) {
        this.config = config
        isRegistered = true
        Log.i(TAG, "SIP ready for ${config.extension} @ ${config.serverIp}:${config.port}")
        // TODO: Integrate PJSIP/baresip AAR here for real REGISTER/INVITE/BYE
    }

    fun onGsmCallActive() {
        if (!isRegistered) {
            Log.w(TAG, "SIP not initialized")
            return
        }
        isCallActive = true
        Log.i(TAG, "Bridging GSM call to SIP")
        // TODO: Trigger SIP INVITE toward PBX
    }

    fun onGsmCallEnded() {
        isCallActive = false
        Log.i(TAG, "Terminating SIP bridge")
        // TODO: Send SIP BYE
    }

    fun terminate() {
        isRegistered = false
        isCallActive = false
        config = null
        Log.i(TAG, "SIP terminated")
    }

    fun isReady(): Boolean = isRegistered && !isCallActive
}
