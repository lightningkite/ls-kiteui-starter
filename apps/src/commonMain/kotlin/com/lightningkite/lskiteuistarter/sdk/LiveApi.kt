package com.lightningkite.lskiteuistarter.sdk

import com.lightningkite.lightningserver.HttpMethod
import com.lightningkite.lightningserver.typed.Fetcher
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable

class LiveApi(val fetcher: Fetcher) : Api {
	override fun withHeaderCalculator(calculator: suspend () -> List<Pair<String, String>>): LiveApi = 
		LiveApi(fetcher.withHeaderCalculator(calculator))
	override suspend fun exampleEndpoint(): kotlin.Int =
		fetcher("example-endpoint", HttpMethod.GET, kotlin.Unit.serializer(), kotlin.Unit, kotlin.Int.serializer())
	override suspend fun exampleEndpoint(input: kotlin.Int): kotlin.Int =
		fetcher("example-endpoint", HttpMethod.POST, kotlin.Int.serializer(), input, kotlin.Int.serializer())

	override val uploadEarlyEndpoint = com.lightningkite.lightningserver.files.LiveClientUploadEarlyEndpoints(fetcher, "upload-early", )

	override val appRelease = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "app-releases", com.lightningkite.lskiteuistarter.AppRelease.serializer(), com.lightningkite.lskiteuistarter.AppRelease.ID.serializer())

	override val user = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpointsAndUpdatesWebsocket(fetcher, "users/rest", com.lightningkite.lskiteuistarter.User.serializer(), com.lightningkite.lskiteuistarter.User.ID.serializer())

	inner class LiveUserAuthApi : Api.UserAuthApi, com.lightningkite.lightningserver.sessions.proofs.AuthClientEndpoints<com.lightningkite.lskiteuistarter.User, com.lightningkite.lskiteuistarter.User.ID> by com.lightningkite.lightningserver.sessions.proofs.LiveAuthClientEndpoints(fetcher, "auth/session", com.lightningkite.lskiteuistarter.User.serializer(), com.lightningkite.lskiteuistarter.User.ID.serializer()), com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lightningserver.sessions.Session<com.lightningkite.lskiteuistarter.User, com.lightningkite.lskiteuistarter.User.ID>, kotlin.uuid.Uuid> by com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "auth/session/sessions", com.lightningkite.lightningserver.sessions.Session.serializer(com.lightningkite.lskiteuistarter.User.serializer(), com.lightningkite.lskiteuistarter.User.ID.serializer()), kotlin.uuid.Uuid.serializer()) {

		inner class LiveEmailApi : Api.UserAuthApi.EmailApi, com.lightningkite.lightningserver.sessions.proofs.ProofClientEndpoints.Email by com.lightningkite.lightningserver.sessions.proofs.LiveProofClientEndpoints.Email(fetcher, "auth/proof/email", ) {
			override suspend fun verifyNewEmail(input: com.lightningkite.services.data.EmailAddress): kotlin.String =
				fetcher("auth/proof/email/verify-new-email", HttpMethod.POST, com.lightningkite.services.data.EmailAddress.serializer(), input, kotlin.String.serializer())
		}
		override val email = LiveEmailApi()

		inner class LiveTimeBasedOTPProof : Api.UserAuthApi.TimeBasedOTPProof, com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lightningserver.sessions.TotpSecret, kotlin.uuid.Uuid> by com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "auth/proof/totp/secrets", com.lightningkite.lightningserver.sessions.TotpSecret.serializer(), kotlin.uuid.Uuid.serializer()), com.lightningkite.lightningserver.sessions.proofs.ProofClientEndpoints.TimeBasedOTP by com.lightningkite.lightningserver.sessions.proofs.LiveProofClientEndpoints.TimeBasedOTP(fetcher, "auth/proof/totp", ) {
		}
		override val totp = LiveTimeBasedOTPProof()

		inner class LivePasswordProof : Api.UserAuthApi.PasswordProof, com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lightningserver.sessions.PasswordSecret, kotlin.uuid.Uuid> by com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "auth/proof/password/secrets", com.lightningkite.lightningserver.sessions.PasswordSecret.serializer(), kotlin.uuid.Uuid.serializer()), com.lightningkite.lightningserver.sessions.proofs.ProofClientEndpoints.Password by com.lightningkite.lightningserver.sessions.proofs.LiveProofClientEndpoints.Password(fetcher, "auth/proof/password", ) {
		}
		override val password = LivePasswordProof()

		override val backupCode = com.lightningkite.lightningserver.sessions.proofs.LiveProofClientEndpoints.BackupCode(fetcher, "auth/proof/backup-codes", )
	}
	override val userAuth = LiveUserAuthApi()

	inner class LiveFcmTokenApi : Api.FcmTokenApi, com.lightningkite.lightningserver.typed.ClientModelRestEndpoints<com.lightningkite.lskiteuistarter.FcmToken, com.lightningkite.lskiteuistarter.FcmToken.ID> by com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "fcmTokens", com.lightningkite.lskiteuistarter.FcmToken.serializer(), com.lightningkite.lskiteuistarter.FcmToken.ID.serializer()) {
		override suspend fun registerToken(input: com.lightningkite.lskiteuistarter.FcmToken.ID): com.lightningkite.services.database.EntryChange<com.lightningkite.lskiteuistarter.FcmToken> =
			fetcher("fcmTokens/register", HttpMethod.POST, com.lightningkite.lskiteuistarter.FcmToken.ID.serializer(), input, com.lightningkite.services.database.EntryChange.serializer(com.lightningkite.lskiteuistarter.FcmToken.serializer()))
		override suspend fun testInAppNotifications(id: com.lightningkite.lskiteuistarter.FcmToken.ID): kotlin.String =
			fetcher("fcmTokens/${fetcher.url(id, com.lightningkite.lskiteuistarter.FcmToken.ID.serializer())}/test", HttpMethod.POST, kotlin.Unit.serializer(), kotlin.Unit, kotlin.String.serializer())
		override suspend fun clearToken(id: com.lightningkite.lskiteuistarter.FcmToken.ID): kotlin.Boolean =
			fetcher("fcmTokens/${fetcher.url(id, com.lightningkite.lskiteuistarter.FcmToken.ID.serializer())}/clear", HttpMethod.POST, kotlin.Unit.serializer(), kotlin.Unit, kotlin.Boolean.serializer())
	}
	override val fcmToken = LiveFcmTokenApi()

	override val organization = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "organizations", com.lightningkite.lskiteuistarter.Organization.serializer(), com.lightningkite.lskiteuistarter.Organization.ID.serializer())

	override val membership = com.lightningkite.lightningserver.typed.LiveClientModelRestEndpoints(fetcher, "memberships", com.lightningkite.lskiteuistarter.Membership.serializer(), com.lightningkite.lskiteuistarter.Membership.ID.serializer())

	inner class LiveMetaApi : Api.MetaApi {
		override suspend fun getServerHealth(): com.lightningkite.lightningserver.typed.ServerHealth =
			fetcher("meta/health", HttpMethod.GET, kotlin.Unit.serializer(), kotlin.Unit, com.lightningkite.lightningserver.typed.ServerHealth.serializer())
		override suspend fun bulkRequest(input: Map<String, com.lightningkite.lightningserver.typed.BulkRequest>): Map<String, com.lightningkite.lightningserver.typed.BulkResponse> =
			fetcher("meta/bulk", HttpMethod.POST, MapSerializer(String.serializer(), com.lightningkite.lightningserver.typed.BulkRequest.serializer()), input, MapSerializer(String.serializer(), com.lightningkite.lightningserver.typed.BulkResponse.serializer()))
	}
	override val meta = LiveMetaApi()
}
