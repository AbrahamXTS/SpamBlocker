package net.abrahamespinosa.spam_blocker.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

class SmsSender(private val context: Context) {
    fun send(address: String, body: String) {
        check(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED,
        ) { "No hay permiso para enviar SMS" }

        val subscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId()
        val manager = if (SubscriptionManager.isValidSubscriptionId(subscriptionId)) {
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        } else {
            SmsManager.getDefault()
        }
        manager.sendTextMessage(address, null, body, null, null)
    }
}