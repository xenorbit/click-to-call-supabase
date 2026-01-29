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
 * Repository for communicating with Supabase Edge Functions
 * Handles device registration and API calls
 */
class SupabaseRepository {

    companion object {
        private const val TAG = "SupabaseRepository"
        
        // TODO: Replace with your Supabase project URL
        private const val SUPABASE_URL = "https://yfqagnhsiamagtewxxiz.supabase.co"
        
        // Supabase anon key for API authentication
        private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlmcWFnbmhzaWFtYWd0ZXd4eGl6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njg0NTM0NTUsImV4cCI6MjA4NDAyOTQ1NX0.5s2XwssFnB_eAnkJRcvDz8wwwXj7y8lcHWPIxcON0-g"
        
        // Edge Function endpoints
        private const val REGISTER_DEVICE_URL = "$SUPABASE_URL/functions/v1/register-device"
    }

    /**
     * Register device with Supabase Edge Function
     * Associates the FCM token with the pairing code
     * 
     * @param pairingCode 6-digit code displayed to user
     * @param fcmToken Firebase Cloud Messaging token
     * @param deviceName Name of the device (e.g., "Pixel 8 Pro")
     * @return Result with deviceId on success, or error
     */
    suspend fun registerDevice(
        pairingCode: String,
        fcmToken: String,
        deviceName: String = "Android Device"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Registering device with pairing code: $pairingCode")
            
            val url = URL(REGISTER_DEVICE_URL)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
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
                    "Registration failed with code: $responseCode"
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
     * Check if device is paired by querying the server
     * @param pairingCode The 6-digit pairing code
     * @return Result with true if paired, false if not
     */
    suspend fun checkPairingStatus(pairingCode: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking pairing status for code: $pairingCode")
            
            val url = URL("$SUPABASE_URL/functions/v1/check-pairing-status")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val jsonBody = JSONObject().apply {
                put("pairingCode", pairingCode)
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
                val isPaired = response.optBoolean("isPaired", false)
                Log.d(TAG, "Pairing status: $isPaired")
                Result.success(isPaired)
            } else {
                Log.e(TAG, "Check status failed: $responseCode - $responseBody")
                Result.failure(Exception("Failed to check status"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Network error checking status", e)
            Result.failure(e)
        }
    }
}
