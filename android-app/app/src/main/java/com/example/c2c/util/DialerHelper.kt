package com.example.c2c.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object DialerHelper {
    
    /**
     * Opens the phone dialer with the given number pre-filled.
     * Uses ACTION_DIAL which opens the dialer without starting the call.
     * The user must press the call button to initiate the call.
     *
     * @param context The context to start the activity
     * @param phoneNumber The phone number to dial
     */
    fun openDialer(context: Context, phoneNumber: String) {
        val cleanNumber = cleanPhoneNumber(phoneNumber)
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Cleans the phone number by removing non-digit characters except +
     */
    private fun cleanPhoneNumber(phoneNumber: String): String {
        val hasPlus = phoneNumber.startsWith("+")
        val digitsOnly = phoneNumber.filter { it.isDigit() }
        return if (hasPlus) "+$digitsOnly" else digitsOnly
    }

    /**
     * Formats a phone number for display
     */
    fun formatForDisplay(phoneNumber: String): String {
        val cleaned = cleanPhoneNumber(phoneNumber)
        return when {
            cleaned.length == 10 -> {
                "(${cleaned.substring(0, 3)}) ${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
            }
            cleaned.length == 11 && cleaned.startsWith("1") -> {
                "+1 (${cleaned.substring(1, 4)}) ${cleaned.substring(4, 7)}-${cleaned.substring(7)}"
            }
            cleaned.startsWith("+") && cleaned.length > 10 -> {
                val country = cleaned.substring(0, cleaned.length - 10)
                val number = cleaned.substring(cleaned.length - 10)
                "$country (${number.substring(0, 3)}) ${number.substring(3, 6)}-${number.substring(6)}"
            }
            else -> cleaned
        }
    }
}
