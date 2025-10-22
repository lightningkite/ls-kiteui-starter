package com.lightningkite.lskiteuistarter.sdk

import com.lightningkite.kiteui.exceptions.ExceptionMessage
import com.lightningkite.kiteui.exceptions.ExceptionToMessage
import com.lightningkite.kiteui.exceptions.ExceptionToMessage.Companion.invoke
import com.lightningkite.kiteui.exceptions.ExceptionToMessages
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.PersistentProperty
import com.lightningkite.kiteui.suppressConnectivityIssues
import com.lightningkite.lightningserver.*
import com.lightningkite.lightningserver.auth.accessToken
import com.lightningkite.lightningserver.db.ModelCache
import com.lightningkite.lightningserver.sessions.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import com.lightningkite.services.data.*
import com.lightningkite.services.database.*
import com.lightningkite.services.files.*
import com.lightningkite.lskiteuistarter.User
import com.lightningkite.lskiteuistarter.fcmToken
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.coroutines.cancellation.CancellationException


class UserSession(
    val api: Api,
    val userId: Uuid,
) : CachedApi(api) {

}
