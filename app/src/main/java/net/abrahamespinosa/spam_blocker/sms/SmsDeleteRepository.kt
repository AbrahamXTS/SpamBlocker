package net.abrahamespinosa.spam_blocker.sms

import android.content.ContentUris
import android.content.ContentResolver
import android.provider.Telephony
import android.provider.BaseColumns
import net.abrahamespinosa.spam_blocker.data.InboxMessageDao
import net.abrahamespinosa.spam_blocker.data.InboxMessageEntity

data class ConversationDeleteResult(
    val deletedCount: Int,
    val failedCount: Int,
)

data class SmsDeleteResult(
    val providerConfirmed: Boolean,
)

class SmsDeleteRepository(
    private val contentResolver: ContentResolver,
    private val inboxMessageDao: InboxMessageDao,
) {
    suspend fun delete(message: InboxMessageEntity): SmsDeleteResult {
        val providerConfirmed = runCatching { deleteProviderMessage(message) }.isSuccess
        inboxMessageDao.deleteById(message.id)
        return SmsDeleteResult(providerConfirmed)
    }

    suspend fun deleteConversation(address: String): ConversationDeleteResult {
        return deleteConversation(inboxMessageDao.findByAddress(address))
    }

    suspend fun deleteConversation(messages: List<InboxMessageEntity>): ConversationDeleteResult {
        val deletedIds = mutableListOf<Long>()
        var failedCount = 0
        messages.forEach { message ->
            val providerConfirmed = runCatching { deleteProviderMessage(message) }.isSuccess
            if (providerConfirmed) {
                deletedIds += message.id
            } else {
                failedCount++
                deletedIds += message.id
            }
        }
        if (deletedIds.isNotEmpty()) inboxMessageDao.deleteByIds(deletedIds)
        return ConversationDeleteResult(deletedIds.size, failedCount)
    }

    private suspend fun deleteProviderMessage(message: InboxMessageEntity) {
        val providerId = message.providerId ?: resolveProviderId(message)
        val deletedByUri = contentResolver.delete(
            ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, providerId),
            null,
            null,
        )
        val deleted = if (deletedByUri == 0) {
            contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${BaseColumns._ID} = ?",
                arrayOf(providerId.toString()),
            )
        } else {
            deletedByUri
        }
        check(deleted == 1) { "El proveedor SMS no eliminó el mensaje" }
    }

    private suspend fun resolveProviderId(message: InboxMessageEntity): Long {
        val lowerBound = message.receivedAt - RESOLUTION_WINDOW_MS
        val upperBound = message.receivedAt + RESOLUTION_WINDOW_MS
        var bestId: Long? = null
        var bestDistance = Long.MAX_VALUE
        contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.DATE),
            "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.BODY} = ? " +
                "AND ${Telephony.Sms.DATE} BETWEEN ? AND ?",
            arrayOf(
                message.address,
                message.body,
                lowerBound.toString(),
                upperBound.toString(),
            ),
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (cursor.moveToNext()) {
                val date = cursor.getLong(dateIndex)
                val distance = kotlin.math.abs(date - message.receivedAt)
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestId = cursor.getLong(idIndex)
                }
            }
        }
        val resolvedId = bestId ?: error("No se encontró el SMS en el proveedor del sistema")
        inboxMessageDao.updateProviderId(message.id, resolvedId)
        return resolvedId
    }

    private companion object {
        const val RESOLUTION_WINDOW_MS = 5 * 60 * 1000L
    }
}