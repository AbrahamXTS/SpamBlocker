package net.abrahamespinosa.spam_blocker.notifications

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InboxNotificationTest {
    @Test
    fun threadIntentTargetsInboxConversation() {
        val intent = InboxNotification.threadIntent(
            context = ApplicationProvider.getApplicationContext(),
            address = "+5215550100",
            requestId = 42L,
        )

        assertEquals(InboxNotification.actionOpenThread, intent.action)
        assertEquals("+5215550100", intent.getStringExtra(InboxNotification.extraThreadAddress))
        assertEquals(42L, intent.getLongExtra(InboxNotification.extraRequestId, Long.MIN_VALUE))
        assertTrue(intent.flags and android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(intent.flags and android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
    }
}
