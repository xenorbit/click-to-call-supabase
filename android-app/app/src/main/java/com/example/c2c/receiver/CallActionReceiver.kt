package com.example.c2c.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.c2c.service.CallNotificationService

class CallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.example.c2c.ACTION_DISMISS"
        const val ACTION_CALL = "com.example.c2c.ACTION_CALL"
        const val EXTRA_PHONE_NUMBER = "phone_number"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Dismiss the notification first
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(CallNotificationService.NOTIFICATION_ID)
        
        when (intent.action) {
            ACTION_CALL -> {
                // Make the call
                val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)
                if (!phoneNumber.isNullOrEmpty()) {
                    val callIntent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:$phoneNumber")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(callIntent)
                }
            }
            ACTION_DISMISS -> {
                // Just dismiss - already done above
            }
        }
    }
}
