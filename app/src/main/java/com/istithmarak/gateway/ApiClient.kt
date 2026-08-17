package com.istithmarak.gateway

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val PREFS_NAME = "gateway_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val DEFAULT_SERVER_URL = "http://127.0.0.1:3000"

    fun getServerUrl(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun saveServerUrl(context: Context, url: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }

    fun get(context: Context, path: String): String {
        val url = URL(getServerUrl(context) + path)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    fun post(context: Context, path: String, jsonBody: JSONObject): String {
        val url = URL(getServerUrl(context) + path)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 5000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.outputStream.use { os ->
            os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    fun delete(context: Context, path: String): String {
        val url = URL(getServerUrl(context) + path)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "DELETE"
        conn.connectTimeout = 5000
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    fun getNumbers(context: Context): JSONArray {
        val res = get(context, "/api/numbers")
        return JSONArray(res)
    }

    fun getMessages(context: Context): JSONArray {
        val res = get(context, "/api/messages")
        return JSONArray(res)
    }

    fun addNumber(context: Context, phone: String, name: String): Boolean {
        val body = JSONObject().apply {
            put("phone", phone)
            put("name", name)
        }
        return try {
            val res = post(context, "/api/add-number", body)
            JSONObject(res).optBoolean("success", false)
        } catch (e: Exception) {
            false
        }
    }

    fun deleteNumber(context: Context, phone: String): Boolean {
        return try {
            val res = delete(context, "/api/delete-number/$phone")
            JSONObject(res).optBoolean("success", false)
        } catch (e: Exception) {
            false
        }
    }

    fun sendMessage(context: Context, phone: String, message: String, type: String = "instant"): Boolean {
        val body = JSONObject().apply {
            put("phone", phone)
            put("message", message)
            put("type", type)
        }
        return try {
            val res = post(context, "/api/send-message", body)
            JSONObject(res).optBoolean("success", false)
        } catch (e: Exception) {
            false
        }
    }
}
