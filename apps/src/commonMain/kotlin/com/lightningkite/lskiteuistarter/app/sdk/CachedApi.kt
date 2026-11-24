package com.lightningkite.lskiteuistarter.app.sdk

import com.lightningkite.lightningserver.db.*
import kotlinx.serialization.builtins.*
import kotlin.time.Duration.Companion.minutes

open class CachedApi(
	val uncached: Api,
	defaultRefreshPolicy: RefreshPolicy = RefreshPolicy.maxAge(5.minutes),
) {
	open val appReleases = ModelCache(uncached.appRelease, com.lightningkite.lskiteuistarter.AppRelease.serializer(), defaultRefreshPolicy = defaultRefreshPolicy)
	open val users = ModelCache(uncached.user, com.lightningkite.lskiteuistarter.User.serializer(), defaultRefreshPolicy = defaultRefreshPolicy)
	open val sessions = ModelCache(uncached.userAuth, com.lightningkite.lightningserver.sessions.Session.serializer(com.lightningkite.lskiteuistarter.User.serializer(), com.lightningkite.lskiteuistarter.User.ID.serializer()), defaultRefreshPolicy = defaultRefreshPolicy)
	open val totpSecrets = ModelCache(uncached.userAuth.totp, com.lightningkite.lightningserver.sessions.TotpSecret.serializer(), defaultRefreshPolicy = defaultRefreshPolicy)
	open val passwordSecrets = ModelCache(uncached.userAuth.password, com.lightningkite.lightningserver.sessions.PasswordSecret.serializer(), defaultRefreshPolicy = defaultRefreshPolicy)
	open val webAuthNCredentials = ModelCache(uncached.userAuth.webAuthN, com.lightningkite.lightningserver.sessions.proofs.WebAuthNCredential.serializer(), defaultRefreshPolicy = defaultRefreshPolicy)
	open val fcmTokens = ModelCache(uncached.fcmToken, com.lightningkite.lskiteuistarter.FcmToken.serializer(), defaultRefreshPolicy = defaultRefreshPolicy)
	open val organizations = ModelCache(uncached.organization, com.lightningkite.lskiteuistarter.Organization.serializer(), defaultRefreshPolicy = defaultRefreshPolicy)
	open val memberships = ModelCache(uncached.membership, com.lightningkite.lskiteuistarter.Membership.serializer(), defaultRefreshPolicy = defaultRefreshPolicy)
}
