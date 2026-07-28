package com.istithmarak.gateway

import android.app.Application
import android.util.Log

class GatewayApplication : Application() {
    companion object {
        private const val TAG = "GatewayApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Gateway Application initialized")
    }
}
