package net.abrahamespinosa.spam_blocker.sms

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsMessageRouterTest {
    @Test
    fun matchingKeywordGoesToQuarantine() {
        assertEquals(
            SmsDestination.QUARANTINE,
            SmsMessageRouter.destination("Gran oferta", listOf("oferta")),
        )
    }

    @Test
    fun nonMatchingMessageGoesToInbox() {
        assertEquals(
            SmsDestination.INBOX,
            SmsMessageRouter.destination("Hola", listOf("oferta")),
        )
    }
}