package com.lightningkite.lskiteuistarter.utils

import com.lightningkite.kiteui.locale.renderToString
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

fun Instant.toRelativeTimeString(): String {
    val diff = Clock.System.now() - this
    return when {
        diff < 1.minutes -> "just now"
        diff < 60.minutes -> "${diff.inWholeMinutes}m ago"
        diff < 24.hours -> "${diff.inWholeHours}h ago"
        diff < 7.days -> "${diff.inWholeDays}d ago"
        else -> renderToString()
    }
}
