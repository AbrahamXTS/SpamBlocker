package net.abrahamespinosa.spam_blocker.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.abrahamespinosa.spam_blocker.SpamBlockerApplication
import net.abrahamespinosa.spam_blocker.data.InboxMessageEntity
import net.abrahamespinosa.spam_blocker.data.QuarantineItemEntity
import net.abrahamespinosa.spam_blocker.notifications.InboxNotification
import net.abrahamespinosa.spam_blocker.sms.SmsDestination
import net.abrahamespinosa.spam_blocker.sms.SmsMessageRouter

class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val application = context.applicationContext as SpamBlockerApplication
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val address = messages.firstOrNull()?.originatingAddress ?: return pendingResult.finish()
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = application.database
                val destination = SmsMessageRouter.destination(body, database.keywordDao().getWords())
                if (destination == SmsDestination.QUARANTINE) {
                    database.quarantineDao().insert(
                        QuarantineItemEntity(
                            type = "sms",
                            address = address,
                            body = body,
                            receivedAt = System.currentTimeMillis(),
                        ),
                    )
                } else {
                    val providerUri = context.contentResolver.insert(
                        Telephony.Sms.Inbox.CONTENT_URI,
                        android.content.ContentValues().apply {
                            put(Telephony.Sms.ADDRESS, address)
                            put(Telephony.Sms.BODY, body)
                            put(Telephony.Sms.DATE, System.currentTimeMillis())
                            put(Telephony.Sms.READ, 0)
                        },
                    )
                    database.inboxMessageDao().insertIgnore(
                        InboxMessageEntity(
                            address = address,
                            body = body,
                            receivedAt = System.currentTimeMillis(),
                            providerId = providerUri?.lastPathSegment?.toLongOrNull(),
                        ),
                    )
                    InboxNotification.show(context, address, body)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}