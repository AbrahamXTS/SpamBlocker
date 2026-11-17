package net.abrahamespinosa.spam_blocker.ui.home

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationKeyTest {
    @Test
    fun equivalentPhoneAddressFormatsShareConversationKey() {
        assertEquals(
            conversationKey("+15550100"),
            conversationKey("15550100"),
        )
    }
}
