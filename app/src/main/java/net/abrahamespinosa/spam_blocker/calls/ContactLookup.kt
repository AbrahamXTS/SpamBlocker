package net.abrahamespinosa.spam_blocker.calls

import android.content.ContentResolver
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils

class ContactLookup(private val contentResolver: ContentResolver) {
    fun isContact(number: String?): Boolean {
        if (number.isNullOrBlank()) return false

        val normalizedNumber = PhoneNumberUtils.normalizeNumber(number)
        contentResolver.query(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                .appendPath(normalizedNumber)
                .build(),
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null,
        )?.use { cursor -> return cursor.moveToFirst() }

        return false
    }
}