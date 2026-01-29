package com.example.c2c.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.c2c.R
import com.example.c2c.receiver.CallActionReceiver
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CallNotificationService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "call_requests_v2"  // Changed to force new channel with correct settings
        const val CHANNEL_NAME = "Call Requests"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        android.util.Log.d("C2C_CALL", "★★★ FCM MESSAGE RECEIVED ★★★")
        android.util.Log.d("C2C_CALL", "From: ${remoteMessage.from}")
        android.util.Log.d("C2C_CALL", "Data: ${remoteMessage.data}")

        // Check if message contains data
        remoteMessage.data.let { data ->
            val type = data["type"]
            val phoneNumber = data["phoneNumber"]
            
            android.util.Log.d("C2C_CALL", "Type: $type, Phone: $phoneNumber")

            if (type == "CALL_REQUEST" && !phoneNumber.isNullOrEmpty()) {
                android.util.Log.d("C2C_CALL", ">>> VALID CALL REQUEST - SHOWING NOTIFICATION <<<")
                showCallNotification(phoneNumber)
            } else {
                android.util.Log.d("C2C_CALL", ">>> INVALID: Missing type or phone number <<<")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("FCM_DEBUG", "=== NEW FCM TOKEN ===")
        android.util.Log.d("FCM_DEBUG", "Token: $token")
        // TODO: Send token to server to associate with user
        // This will be handled by the pairing flow
    }

    private fun showCallNotification(phoneNumber: String) {
        android.util.Log.d("C2C_CALL", "▶▶▶ BUILDING NOTIFICATION FOR: $phoneNumber ◀◀◀")
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Ensure notification channel exists
        createNotificationChannel()
        android.util.Log.d("C2C_CALL", "✓ Notification channel created/verified")

        // Create direct call intent - tapping notification will place the call directly
        // Using Activity PendingIntent for reliable behavior when tapping notification body
        val directCallIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        android.util.Log.d("C2C_CALL", "✓ Direct call intent created for: $phoneNumber")
        
        val directCallPendingIntent = PendingIntent.getActivity(
            applicationContext,
            phoneNumber.hashCode(),
            directCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        android.util.Log.d("C2C_CALL", "✓ Direct Call PendingIntent created")

        // Create dial intent for those who want to just open dialer first
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val dialPendingIntent = PendingIntent.getActivity(
            applicationContext,
            phoneNumber.hashCode() + 1,
            dialIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create dismiss intent
        val dismissIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification - tap to call directly, with options to dial or dismiss
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_phone)
            .setContentTitle("📞 Call: $phoneNumber")
            .setContentText("Tap to call directly")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(directCallPendingIntent)
            .addAction(R.drawable.ic_phone, "Call Now", directCallPendingIntent)
            .addAction(R.drawable.ic_phone, "Open Dialer", dialPendingIntent)
            .addAction(R.drawable.ic_close, "Dismiss", dismissPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        android.util.Log.d("C2C_CALL", "★★★ NOTIFICATION POSTED SUCCESSFULLY ★★★")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming call requests from browser"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
