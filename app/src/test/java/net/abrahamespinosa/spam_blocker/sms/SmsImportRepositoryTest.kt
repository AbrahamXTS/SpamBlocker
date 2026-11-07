package net.abrahamespinosa.spam_blocker.sms

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsImportRepositoryTest {
    @Test
    fun historicalProviderMessageMapsWithoutKeywordFiltering() {
        val message = ProviderSms(
            providerId = 42L,
            threadId = 7L,
            address = "5551234",
            body = "Gran oferta",
            receivedAt = 100L,
        )

        val entity = message.toInboxEntity()

        assertEquals(42L, entity.providerId)
        assertEquals(7L, entity.threadId)
        assertEquals("Gran oferta", entity.body)
    }
}