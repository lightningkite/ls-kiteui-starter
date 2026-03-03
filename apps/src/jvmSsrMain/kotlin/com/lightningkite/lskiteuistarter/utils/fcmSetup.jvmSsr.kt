// by Claude — jvmSsr stubs for FCM (not available on headless JVM)
package com.lightningkite.lskiteuistarter.utils

actual fun fcmSetup(): Unit {}
actual suspend fun requestNotificationPermissions(): Unit {}
actual suspend fun notificationPermissions(): Boolean? = false
