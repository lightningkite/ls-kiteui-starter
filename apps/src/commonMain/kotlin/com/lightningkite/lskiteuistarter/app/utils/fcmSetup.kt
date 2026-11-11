package com.lightningkite.lskiteuistarter.app.utils


expect fun fcmSetup(): Unit

expect suspend fun requestNotificationPermissions(): Unit

expect suspend fun notificationPermissions(): Boolean?
