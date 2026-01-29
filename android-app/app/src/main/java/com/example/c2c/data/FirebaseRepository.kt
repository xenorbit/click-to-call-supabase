package com.example.c2c.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Repository for communicating with Firebase Cloud Functions
 */
class FirebaseRepository {

    companion object {
        private const val TAG = "FirebaseRepository"
        private const val BASE_URL = "https://us-central1-c2c-beta.cloudfunctions.net"
    }

    /**
     * Register device with Firebase Cloud Functions
     * This associates the FCM token with the pairing code
     */
    suspend fun registerDevice(
        pairingCode: String,
        fcmToken: String,
        deviceName: String = "Android Device"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/registerDevice")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val jsonBody = JSONObject().apply {
                put("pairingCode", pairingCode)
                put("fcmToken", fcmToken)
                put("deviceName", deviceName)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseBody = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
            }

            connection.disconnect()

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = JSONObject(responseBody)
                val deviceId = response.optString("deviceId", "")
                Log.d(TAG, "Device registered successfully: $deviceId")
                Result.success(deviceId)
            } else {
                val error = try {
                    JSONObject(responseBody).optString("error", "Registration failed")
                } catch (e: Exception) {
                    "Registration failed"
                }
                Log.e(TAG, "Registration failed: $error")
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Network error during registration", e)
            Result.failure(e)
        }
    }

    /**
     * Test the connection to Firebase
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/pairDevice")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "OPTIONS"
            connection.connectTimeout = 5000
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode == 204 || responseCode == 200
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            false
        }
    }
}
