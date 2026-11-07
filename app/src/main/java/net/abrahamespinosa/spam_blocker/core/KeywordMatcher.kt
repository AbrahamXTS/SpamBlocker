package net.abrahamespinosa.spam_blocker.core

import java.util.Locale

object KeywordMatcher {
    fun containsKeyword(body: String, keywords: Collection<String>): Boolean {
        val normalizedBody = body.lowercase(Locale.getDefault())
        return keywords
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .any { normalizedBody.contains(it.lowercase(Locale.getDefault())) }
    }
}