package com.lightningkite.template.sdk

import com.lightningkite.UUID
import com.lightningkite.default
import com.lightningkite.kiteui.Async
import com.lightningkite.kiteui.asyncGlobal
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.lightningdb.CollectionUpdates
import com.lightningkite.lightningdb.Query
import com.lightningkite.lightningdb.condition
import com.lightningkite.lightningdb.sort
import com.lightningkite.lightningserver.db.ModelCache
import com.lightningkite.template.User
import kotlinx.datetime.Clock
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Duration.Companion.minutes


class UserSession(
    override val api: Api,
    override val userToken: String,
    override val userAccessToken: suspend () -> String,
    self: User,
) : AbstractUserSession(api, userToken, userAccessToken) {

    val userId = self._id
    val role = self.role

    val nonCached = object : AbstractUserSession(api, userToken, userAccessToken) {
        override val api: Api = this@UserSession.api
        override val userToken: String = this@UserSession.userToken
        override val userAccessToken: suspend () -> String = this@UserSession.userAccessToken
    }

    val users = ModelCache(nonCached.user, User.serializer())
}

val sessionToken = PersistentProperty<String?>("sessionToken", null)
var invalidateAccess = BasicListenable()


val userToken: Readable<Triple<LiveApi, String, suspend () -> String>?> = shared {
    rerunOn(invalidateAccess)
    val refresh = sessionToken<String?>() ?: return@shared null
    val api = selectedApi().api

    var lastRefresh: Instant = now()
    var token: Async<String> = asyncGlobal {
        api.userAuth.getTokenSimple(refresh)
    }

    Triple(api, refresh, suspend {
        if (lastRefresh <= now().minus(4.minutes)) {
            lastRefresh = now()
            token = asyncGlobal {
                api.userAuth.getTokenSimple(refresh)
            }
        }
        token.await()
    })

}

val currentSession = sharedSuspending<UserSession?> {
    val (api: LiveApi, userToken: String, access: suspend () -> String) = userToken.await() ?: return@sharedSuspending null
    val self = try {
        api.userAuth.getSelf(access, masquerade = null)
    } catch (e: Exception) {
        return@sharedSuspending null
    }
    UserSession(
        api = api,
        userToken = userToken,
        userAccessToken = access,
        self = self,
    )
}

