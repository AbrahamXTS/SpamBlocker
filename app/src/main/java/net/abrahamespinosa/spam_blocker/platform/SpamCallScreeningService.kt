package net.abrahamespinosa.spam_blocker.platform

import android.telecom.Call
import android.telecom.CallScreeningService
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.abrahamespinosa.spam_blocker.SpamBlockerApplication
import net.abrahamespinosa.spam_blocker.calls.ContactLookup
import net.abrahamespinosa.spam_blocker.data.QuarantineItemEntity

class SpamCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val number = callDetails.handle
            ?.takeIf { it.scheme == "tel" }
            ?.schemeSpecificPart
        val isContact = ContactLookup(contentResolver).isContact(number)
        if (isContact) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        respondToCall(
            callDetails,
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(true)
                .setSkipNotification(true)
                .build(),
        )

        val application = application as SpamBlockerApplication
        CoroutineScope(Dispatchers.IO).launch {
            application.database.quarantineDao().insert(
                QuarantineItemEntity(
                    type = "call",
                    address = number.orEmpty(),
                    body = null,
                    receivedAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}