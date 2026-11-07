package net.abrahamespinosa.spam_blocker.sms

import net.abrahamespinosa.spam_blocker.core.KeywordMatcher

enum class SmsDestination {
    INBOX,
    QUARANTINE,
}

object SmsMessageRouter {
    fun destination(body: String, keywords: Collection<String>): SmsDestination =
        if (KeywordMatcher.containsKeyword(body, keywords)) {
            SmsDestination.QUARANTINE
        } else {
            SmsDestination.INBOX
        }
}