package net.abrahamespinosa.spam_blocker.sms

import android.content.ContentResolver
import android.provider.Telephony
import net.abrahamespinosa.spam_blocker.data.InboxMessageDao
import net.abrahamespinosa.spam_blocker.data.InboxMessageEntity
import net.abrahamespinosa.spam_blocker.data.SyncStateDao
import net.abrahamespinosa.spam_blocker.data.SyncStateEntity

data class ProviderSms(
    val providerId: Long,
    val threadId: Long?,
    val address: String,
    val body: String,
    val receivedAt: Long,
)

fun ProviderSms.toInboxEntity(): InboxMessageEntity = InboxMessageEntity(
    providerId = providerId,
    threadId = threadId,
    address = address,
    body = body,
    receivedAt = receivedAt,
)

class SmsImportRepository(
    private val contentResolver: ContentResolver,
    private val inboxMessageDao: InboxMessageDao,
    private val syncStateDao: SyncStateDao,
) {
    suspend fun isInitialImportComplete(): Boolean =
        syncStateDao.get(INITIAL_IMPORT_KEY) == COMPLETE_VALUE

    suspend fun markInitialImportComplete() {
        syncStateDao.put(SyncStateEntity(INITIAL_IMPORT_KEY, COMPLETE_VALUE))
    }

    suspend fun importExistingMessages(): Int {
        var imported = 0
        contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            PROJECTION,
            null,
            null,
            "${Telephony.Sms.DATE} ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIndex = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext()) {
                val providerSms = ProviderSms(
                    providerId = cursor.getLong(idIndex),
                    threadId = threadIndex.takeIf { it >= 0 && !cursor.isNull(it) }
                        ?.let(cursor::getLong),
                    address = cursor.getString(addressIndex).orEmpty(),
                    body = cursor.getString(bodyIndex).orEmpty(),
                    receivedAt = cursor.getLong(dateIndex),
                )
                if (inboxMessageDao.insertIgnore(providerSms.toInboxEntity()) != -1L) {
                    imported++
                }
            }
        }
        return imported
    }

    private companion object {
        const val INITIAL_IMPORT_KEY = "initial_sms_import"
        const val COMPLETE_VALUE = "complete"

        val PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
        )
    }
}