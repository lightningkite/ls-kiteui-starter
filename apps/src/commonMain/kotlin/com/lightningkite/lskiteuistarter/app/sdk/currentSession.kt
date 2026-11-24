package com.lightningkite.lskiteuistarter.app.sdk

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.exceptions.*
import com.lightningkite.kiteui.reactive.PersistentProperty
import com.lightningkite.kiteui.suppressConnectivityIssues
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.lightningserver.LsErrorException
import com.lightningkite.lightningserver.auth.accessTokenFromCookie
import com.lightningkite.lskiteuistarter.FcmToken
import com.lightningkite.lskiteuistarter.app.fcmToken
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactiveSuspending
import com.lightningkite.reactive.core.*
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch


/**
 * The refresh token lives in an HttpOnly cookie the app can't read, so nothing here can notice it change.
 * Bump this after logging in or out to make [currentSession] ask the server again.
 */
val sessionCookieChanged = Signal(0)

// Older versions of the app kept the refresh token here; [currentSession] moves it into the cookie once.
private val legacySessionToken = PersistentProperty<String?>("sessionToken", null)

private val tokenLog = Log.tag("currentSession.token")
val currentSession: Reactive<UserSession?> = rememberSuspending {
    sessionCookieChanged()
    val api = apiOverride() ?: selectedApi().api

    legacySessionToken.value?.let { token ->
        try {
            api.userAuth.setRefreshTokenCookie(token)
        } catch (e: LsErrorException) {
            if (e.error.http != 400 && e.error.http != 401) throw e
            tokenLog.info("Discarding a stored refresh token the server no longer accepts")
        }
        legacySessionToken.value = null
    }

    val accessToken = api.userAuth.accessTokenFromCookie()
    val authApi = api.withMetadata { set("Authorization", accessToken()) }
    val self = try {
        authApi.userAuth.getSelf()
    } catch (e: LsErrorException) {
        if (e.error.http == 401) return@rememberSuspending null
        throw e
    }
    UserSession(
        api = authApi,
        userId = self._id,
    )
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