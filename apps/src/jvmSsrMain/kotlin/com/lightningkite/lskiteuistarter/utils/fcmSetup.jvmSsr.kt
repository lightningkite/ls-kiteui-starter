package com.lightningkite.lskiteuistarter.utils

/** SSR test stub — push notifications are not available in the headless JVM environment. */
actual fun fcmSetup(): Unit = Unit

/** SSR test stub — no notification permission prompt on JVM. */
actual suspend fun requestNotificationPermissions(): Unit = Unit

/** SSR test stub — no notification permission state on JVM. */
actual suspend fun notificationPermissions(): Boolean? = null
