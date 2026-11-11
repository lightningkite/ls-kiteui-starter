package com.lightningkite.lskiteuistarter

import com.lightningkite.services.data.ZonedDateTime
import com.lightningkite.services.data.nowLocal
import kotlin.time.Clock
import kotlin.time.Instant

private var appClock: Clock = Clock.System

val Clock.Companion.App: Clock get() = appClock

@RequiresOptIn("Meant to only be used in tests.")
annotation class TestOnly

@TestOnly
fun setAppClockForTesting(clock: Clock) {
    println("WARN!! App clock is being set to $clock.")
    appClock = clock
}

fun now(): Instant = Clock.App.now()
fun nowLocal(): ZonedDateTime = Clock.App.nowLocal()