package net.abrahamespinosa.spam_blocker.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeTabNavigationTest {
    @Test
    fun `swiping left advances through the swipeable tabs`() {
        assertEquals(HomeTab.QUARANTINE, HomeTab.INBOX.swipeTo(SwipeDirection.LEFT))
        assertEquals(HomeTab.KEYWORDS, HomeTab.QUARANTINE.swipeTo(SwipeDirection.LEFT))
        assertEquals(HomeTab.STATUS, HomeTab.KEYWORDS.swipeTo(SwipeDirection.LEFT))
        assertNull(HomeTab.STATUS.swipeTo(SwipeDirection.LEFT))
    }

    @Test
    fun `swiping right returns through the swipeable tabs`() {
        assertEquals(HomeTab.KEYWORDS, HomeTab.STATUS.swipeTo(SwipeDirection.RIGHT))
        assertEquals(HomeTab.QUARANTINE, HomeTab.KEYWORDS.swipeTo(SwipeDirection.RIGHT))
        assertEquals(HomeTab.INBOX, HomeTab.QUARANTINE.swipeTo(SwipeDirection.RIGHT))
        assertNull(HomeTab.INBOX.swipeTo(SwipeDirection.RIGHT))
    }
}
