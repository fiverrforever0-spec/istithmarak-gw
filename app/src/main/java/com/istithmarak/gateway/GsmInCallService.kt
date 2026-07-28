package com.istithmarak.gateway

import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

class GsmInCallService : InCallService() {

    companion object {
        var activeCall: Call? = null
            private set
        private const val TAG = "IstithmarakInCall"
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        activeCall = call
        Log.d(TAG, "Call added. State: ${call.state}")

        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, newState: Int) {
                super.onStateChanged(call, newState)
                Log.d(TAG, "Call state changed to: $newState")
                when (newState) {
                    Call.STATE_ACTIVE -> {
                        AudioBridgeEngine.startBridge()
                        SipClientManager.onGsmCallActive()
                    }
                    Call.STATE_DISCONNECTED -> cleanupCall()
                    else -> { /* ignore */ }
                }
            }
        })
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        if (activeCall == call) cleanupCall()
    }

    private fun cleanupCall() {
        AudioBridgeEngine.stopBridge()
        SipClientManager.onGsmCallEnded()
        activeCall = null
        Log.d(TAG, "Call cleanup completed")
    }
}
