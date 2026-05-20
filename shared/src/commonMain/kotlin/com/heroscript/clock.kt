package com.heroscript

import com.lightningkite.services.data.ZonedDateTime
import com.lightningkite.services.data.nowLocal
import kotlin.time.Clock
import kotlin.time.Instant

private var lsKiteuiStarterClock: Clock = Clock.System

val Clock.Companion.lsKiteuiStarter: Clock get() = lsKiteuiStarterClock

@RequiresOptIn("Meant to only be used in tests.")
annotation class TestOnly

@TestOnly
fun setLskiteuistarterClockForTesting(clock: Clock) {
    println("WARN!! heroscript clock is being set to $clock.")
    lsKiteuiStarterClock = clock
}

fun now(): Instant = Clock.lsKiteuiStarter.now()
fun nowLocal(): ZonedDateTime = Clock.lsKiteuiStarter.nowLocal()