package net.abrahamespinosa.spam_blocker.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMatcherTest {
    @Test
    fun emptyKeywordListDoesNotQuarantine() {
        assertFalse(KeywordMatcher.containsKeyword("Gran oferta", emptyList()))
    }

    @Test
    fun matchesCaseInsensitiveSubstring() {
        assertTrue(KeywordMatcher.containsKeyword("Gran OFERTA hoy", listOf("oferta")))
    }

    @Test
    fun doesNotRemoveAccents() {
        assertFalse(KeywordMatcher.containsKeyword("préstamo aprobado", listOf("prestamo")))
    }

    @Test
    fun ignoresBlankKeywords() {
        assertFalse(KeywordMatcher.containsKeyword("Hola", listOf(" ", "")))
    }
}