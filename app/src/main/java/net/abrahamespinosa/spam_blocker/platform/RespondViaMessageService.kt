package net.abrahamespinosa.spam_blocker.platform

import android.app.Service
import android.content.Intent
import android.os.IBinder
import net.abrahamespinosa.spam_blocker.sms.SmsSender

class RespondViaMessageService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val destination = intent?.data?.schemeSpecificPart
        val body = intent?.getStringExtra(Intent.EXTRA_TEXT)
        if (!destination.isNullOrBlank() && !body.isNullOrBlank()) {
            SmsSender(this).send(destination, body)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}