package com.lightningkite.template.sdk

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
import com.lightningkite.template.User
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn


class UserSession(
    val api: Api,
    val userId: Uuid,
) : CachedApi(api) {

}

val sessionToken = PersistentProperty<String?>("sessionToken", null)

val currentSession: Reactive<UserSession?> = rememberSuspending {
    val token = sessionToken() ?: return@rememberSuspending null
    val api = selectedApi().api

    val authApi = api.withHeaderCalculator(api.userAuth.accessToken(token))
    try {
        val self = authApi.userAuth.getSelf()

        UserSession(
            api = authApi,
            userId = self._id,
        ).also {
//            AppScope.launch { registerToken(authApi) }
        }
    } catch (e: Exception) {
        println("FAILED")
        e.printStackTrace()
        null
    }
}

//private suspend fun registerToken(authApi: Api2) {
//    suppressConnectivityIssues {
//        fcmToken()?.takeIf { it.isNotEmpty() }?.let { authApi.fcmToken.registerToken(it) }
//    }
//}
//private suspend fun deregisterToken() {
//    val api: Api2 = selectedApi().api
//    suppressConnectivityIssues {
//        fcmToken()?.takeIf { it.isNotEmpty() }?.let { api.fcmToken.clearToken(it) }
//    }
//}
