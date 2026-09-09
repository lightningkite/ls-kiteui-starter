package com.lightningkite.lskiteuistarter.sdk

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.exceptions.*
import com.lightningkite.kiteui.reactive.PersistentProperty
import com.lightningkite.kiteui.suppressConnectivityIssues
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.lightningserver.LsErrorException
import com.lightningkite.lightningserver.auth.accessToken
import com.lightningkite.lskiteuistarter.FcmToken
import com.lightningkite.lskiteuistarter.fcmToken
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactiveSuspending
import com.lightningkite.reactive.core.*
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch


val sessionToken = PersistentProperty<String?>("sessionToken", null)

val currentSession: Reactive<UserSession?> = rememberSuspending {
    val token = sessionToken() ?: return@rememberSuspending null
    val api = apiOverride() ?: selectedApi().api

    val authApi = api.withHeaderCalculator(api.userAuth.accessToken(token))
    try {
        val self = authApi.userAuth.getSelf()

        UserSession(
            api = authApi,
            userId = self._id,
        )
    } catch (e: Exception) {
        tokenLog.warn("Session fetch failed", e)
        null
    }
}.also { currentSession ->
    AppScope.reactiveSuspending {
        val s = currentSession() ?: run {
            tokenLog.info("Deregstering token logged out")
            deregisterToken()
            return@reactiveSuspending
        }
        suppressConnectivityIssues {
            tokenLog.info("Starting register")
            fcmToken()?.takeIf { it.isNotEmpty() }?.let {
                tokenLog.info("Registering token")
                s.api.fcmToken.registerToken(FcmToken.ID(it))
                tokenLog.info("Registered token")
            }
        }
    }
}
private val tokenLog = Log.tag("currentSession.token")

val currentSessionFailed = BasicListenable()
val currentSessionNotNull = remember {
    val result = currentSession()
    if (result == null) {
        currentSessionFailed.invokeAll()
        launch { deregisterToken() }
        throw CancellationException("No session found")
    }
    result
}

suspend fun deregisterToken() {
    val api: Api = selectedApi().api
    suppressConnectivityIssues {
        fcmToken()?.takeIf { it.isNotEmpty() }?.let { api.fcmToken.clearToken(FcmToken.ID(it)) }
    }
}

fun ExceptionHandlersTree.installLoggedOutErrors() {
    this += ExceptionToMessage<LsErrorException>(3.0f) {
        if (it.error.message != "Session has been terminated.") return@ExceptionToMessage null

        currentSessionFailed.invokeAll()
        ExceptionMessage(
            "Logged Out",
            "You have been logged out, you will be redirected to the Landing page.",
        )
    }
}