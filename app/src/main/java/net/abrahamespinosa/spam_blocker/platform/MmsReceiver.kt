package net.abrahamespinosa.spam_blocker.platform

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.abrahamespinosa.spam_blocker.SpamBlockerApplication
import net.abrahamespinosa.spam_blocker.data.InboxMessageEntity
import net.abrahamespinosa.spam_blocker.notifications.InboxNotification

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val application = context.applicationContext as SpamBlockerApplication
        val address = intent.getStringExtra("address")
            ?: intent.getStringExtra("phone")
            ?: "MMS"
        val receivedAt = System.currentTimeMillis()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching {
                    context.contentResolver.insert(
                        Telephony.Mms.Inbox.CONTENT_URI,
                        ContentValues().apply {
                            put(Telephony.Mms.DATE, receivedAt / 1000)
                            put(Telephony.Mms.READ, 0)
                            put(Telephony.Mms.SEEN, 0)
                        },
                    )
                }
                application.database.inboxMessageDao().insertIgnore(
                    InboxMessageEntity(
                        address = address,
                        body = "MMS recibido",
                        receivedAt = receivedAt,
                    ),
                )
                InboxNotification.show(context, address, "MMS recibido")
            } finally {
                pendingResult.finish()
            }
        }
    }
}