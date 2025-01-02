
package com.lightningkite.template.sdk

import com.lightningkite.*
import com.lightningkite.lightningdb.*
import com.lightningkite.kiteui.*
import kotlinx.datetime.*
import com.lightningkite.serialization.*
import com.lightningkite.lightningserver.db.*
import com.lightningkite.lightningserver.auth.*
import com.lightningkite.lightningserver.serverhealth.ServerHealth
import com.lightningkite.lightningserver.typed.BulkRequest
import com.lightningkite.lightningserver.typed.BulkResponse
import com.lightningkite.template.User
import com.lightningkite.lightningdb.ModelPermissions
import com.lightningkite.lightningdb.Query
import com.lightningkite.lightningdb.QueryPartial
import com.lightningkite.serialization.Partial
import com.lightningkite.UUID
import com.lightningkite.lightningdb.MassModification
import kotlin.Int
import com.lightningkite.lightningdb.Modification
import com.lightningkite.lightningdb.EntryChange
import com.lightningkite.lightningdb.Condition
import kotlin.Unit
import com.lightningkite.lightningdb.GroupCountQuery
import com.lightningkite.lightningdb.AggregateQuery
import kotlin.Double
import com.lightningkite.lightningdb.GroupAggregateQuery
import kotlin.String
import com.lightningkite.lightningserver.auth.proof.FinishProof
import com.lightningkite.lightningserver.auth.proof.Proof
import com.lightningkite.lightningserver.auth.proof.OtpSecret
import com.lightningkite.lightningserver.auth.proof.EstablishOtp
import com.lightningkite.lightningserver.auth.proof.IdentificationAndPassword
import com.lightningkite.lightningserver.auth.proof.PasswordSecret
import com.lightningkite.lightningserver.auth.proof.EstablishPassword
import com.lightningkite.lightningserver.auth.subject.IdAndAuthMethods
import com.lightningkite.lightningserver.auth.subject.LogInRequest
import com.lightningkite.lightningserver.auth.subject.ProofsCheckResult
import com.lightningkite.lightningserver.auth.subject.SubSessionRequest
import com.lightningkite.lightningserver.auth.oauth.OauthTokenRequest
import com.lightningkite.lightningserver.auth.oauth.OauthResponse
import com.lightningkite.lightningserver.auth.subject.Session

class LiveApi(val httpUrl: String, val socketUrl: String): Api {
    override val user: Api.UserApi = LiveUserApi(httpUrl = httpUrl, socketUrl = socketUrl)
    override val emailProof: Api.EmailProofApi = LiveEmailProofApi(httpUrl = httpUrl, socketUrl = socketUrl)
    override val otpSecret: Api.OtpSecretApi = LiveOtpSecretApi(httpUrl = httpUrl, socketUrl = socketUrl)
    override val oneTimePasswordProof: Api.OneTimePasswordProofApi = LiveOneTimePasswordProofApi(httpUrl = httpUrl, socketUrl = socketUrl)
    override val passwordSecret: Api.PasswordSecretApi = LivePasswordSecretApi(httpUrl = httpUrl, socketUrl = socketUrl)
    override val passwordProof: Api.PasswordProofApi = LivePasswordProofApi(httpUrl = httpUrl, socketUrl = socketUrl)
    override val userAuth: Api.UserAuthApi = LiveUserAuthApi(httpUrl = httpUrl, socketUrl = socketUrl)
    override val userSession: Api.UserSessionApi = LiveUserSessionApi(httpUrl = httpUrl, socketUrl = socketUrl)
    override suspend fun getServerHealth(userAccessToken: suspend () -> String, masquerade: String?): ServerHealth = fetch(
        url = "$httpUrl/meta/health",
        method = HttpMethod.GET,
            token = userAccessToken,
            masquerade = masquerade,
    )
    override suspend fun bulkRequest(input: Map<String, BulkRequest>): Map<String, BulkResponse> = fetch(
        url = "$httpUrl/meta/bulk",
        method = HttpMethod.POST,
        body = input
    )
    class LiveUserApi(val httpUrl: String, val socketUrl: String): Api.UserApi {
        override suspend fun default(userAccessToken: suspend () -> String, masquerade: String?): User = fetch(
            url = "$httpUrl/users/_default_",
            method = HttpMethod.GET,
            token = userAccessToken,
            masquerade = masquerade,
        )
        override suspend fun permissions(userAccessToken: suspend () -> String, masquerade: String?): ModelPermissions<User> = fetch(
            url = "$httpUrl/users/_permissions_",
            method = HttpMethod.GET,
            token = userAccessToken,
            masquerade = masquerade,
        )
        override suspend fun query(input: Query<User>, userAccessToken: suspend () -> String, masquerade: String?): List<User> = fetch(
            url = "$httpUrl/users/query",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun queryPartial(input: QueryPartial<User>, userAccessToken: suspend () -> String, masquerade: String?): List<Partial<User>> = fetch(
            url = "$httpUrl/users/query-partial",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun detail(id: UUID, userAccessToken: suspend () -> String, masquerade: String?): User = fetch(
            url = "$httpUrl/users/${id.urlify()}",
            method = HttpMethod.GET,
            token = userAccessToken,
            masquerade = masquerade,
        )
        override suspend fun insertBulk(input: List<User>, userAccessToken: suspend () -> String, masquerade: String?): List<User> = fetch(
            url = "$httpUrl/users/bulk",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun insert(input: User, userAccessToken: suspend () -> String, masquerade: String?): User = fetch(
            url = "$httpUrl/users",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun upsert(id: UUID, input: User, userAccessToken: suspend () -> String, masquerade: String?): User = fetch(
            url = "$httpUrl/users/${id.urlify()}",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun bulkReplace(input: List<User>, userAccessToken: suspend () -> String, masquerade: String?): List<User> = fetch(
            url = "$httpUrl/users",
            method = HttpMethod.PUT,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun replace(id: UUID, input: User, userAccessToken: suspend () -> String, masquerade: String?): User = fetch(
            url = "$httpUrl/users/${id.urlify()}",
            method = HttpMethod.PUT,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun bulkModify(input: MassModification<User>, userAccessToken: suspend () -> String, masquerade: String?): Int = fetch(
            url = "$httpUrl/users/bulk",
            method = HttpMethod.PATCH,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun modifyWithDiff(id: UUID, input: Modification<User>, userAccessToken: suspend () -> String, masquerade: String?): EntryChange<User> = fetch(
            url = "$httpUrl/users/${id.urlify()}/delta",
            method = HttpMethod.PATCH,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun modify(id: UUID, input: Modification<User>, userAccessToken: suspend () -> String, masquerade: String?): User = fetch(
            url = "$httpUrl/users/${id.urlify()}",
            method = HttpMethod.PATCH,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun simplifiedModify(id: UUID, input: Partial<User>, userAccessToken: suspend () -> String, masquerade: String?): User = fetch(
            url = "$httpUrl/users/${id.urlify()}/simplified",
            method = HttpMethod.PATCH,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun bulkDelete(input: Condition<User>, userAccessToken: suspend () -> String, masquerade: String?): Int = fetch(
            url = "$httpUrl/users/bulk-delete",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun delete(id: UUID, userAccessToken: suspend () -> String, masquerade: String?): Unit = fetch(
            url = "$httpUrl/users/${id.urlify()}",
            method = HttpMethod.DELETE,
            token = userAccessToken,
            masquerade = masquerade,
        )
        override suspend fun count(input: Condition<User>, userAccessToken: suspend () -> String, masquerade: String?): Int = fetch(
            url = "$httpUrl/users/count",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun groupCount(input: GroupCountQuery<User>, userAccessToken: suspend () -> String, masquerade: String?): Map<String, Int> = fetch(
            url = "$httpUrl/users/group-count",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun aggregate(input: AggregateQuery<User>, userAccessToken: suspend () -> String, masquerade: String?): Double? = fetch(
            url = "$httpUrl/users/aggregate",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun groupAggregate(input: GroupAggregateQuery<User>, userAccessToken: suspend () -> String, masquerade: String?): Map<String, Double?> = fetch(
            url = "$httpUrl/users/group-aggregate",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
    }
    class LiveEmailProofApi(val httpUrl: String, val socketUrl: String): Api.EmailProofApi {
        override suspend fun beginEmailOwnershipProof(input: String): String = fetch(
            url = "$httpUrl/auth/proof/email/start",
            method = HttpMethod.POST,
            body = input
        )
        override suspend fun proveEmailOwnership(input: FinishProof): Proof = fetch(
            url = "$httpUrl/auth/proof/email/prove",
            method = HttpMethod.POST,
            body = input
        )
    }
    class LiveOtpSecretApi(val httpUrl: String, val socketUrl: String): Api.OtpSecretApi {
        override suspend fun default(anyAccessToken: suspend () -> String, masquerade: String?): OtpSecret = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/_default_",
            method = HttpMethod.GET,
            token = anyAccessToken,
            masquerade = masquerade,
        )
        override suspend fun permissions(anyAccessToken: suspend () -> String, masquerade: String?): ModelPermissions<OtpSecret> = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/_permissions_",
            method = HttpMethod.GET,
            token = anyAccessToken,
            masquerade = masquerade,
        )
        override suspend fun query(input: Query<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<OtpSecret> = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/query",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun queryPartial(input: QueryPartial<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<Partial<OtpSecret>> = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/query-partial",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun detail(id: UUID, anyAccessToken: suspend () -> String, masquerade: String?): OtpSecret = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/${id.urlify()}",
            method = HttpMethod.GET,
            token = anyAccessToken,
            masquerade = masquerade,
        )
        override suspend fun insertBulk(input: List<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<OtpSecret> = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/bulk",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun insert(input: OtpSecret, anyAccessToken: suspend () -> String, masquerade: String?): OtpSecret = fetch(
            url = "$httpUrl/auth/proof/otp/secrets",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun upsert(id: UUID, input: OtpSecret, anyAccessToken: suspend () -> String, masquerade: String?): OtpSecret = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/${id.urlify()}",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun bulkReplace(input: List<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<OtpSecret> = fetch(
            url = "$httpUrl/auth/proof/otp/secrets",
            method = HttpMethod.PUT,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun replace(id: UUID, input: OtpSecret, anyAccessToken: suspend () -> String, masquerade: String?): OtpSecret = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/${id.urlify()}",
            method = HttpMethod.PUT,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun bulkModify(input: MassModification<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Int = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/bulk",
            method = HttpMethod.PATCH,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun modifyWithDiff(id: UUID, input: Modification<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): EntryChange<OtpSecret> = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/${id.urlify()}/delta",
            method = HttpMethod.PATCH,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun modify(id: UUID, input: Modification<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): OtpSecret = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/${id.urlify()}",
            method = HttpMethod.PATCH,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun simplifiedModify(id: UUID, input: Partial<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): OtpSecret = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/${id.urlify()}/simplified",
            method = HttpMethod.PATCH,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun bulkDelete(input: Condition<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Int = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/bulk-delete",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun delete(id: UUID, anyAccessToken: suspend () -> String, masquerade: String?): Unit = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/${id.urlify()}",
            method = HttpMethod.DELETE,
            token = anyAccessToken,
            masquerade = masquerade,
        )
        override suspend fun count(input: Condition<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Int = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/count",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun groupCount(input: GroupCountQuery<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Map<String, Int> = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/group-count",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun aggregate(input: AggregateQuery<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Double? = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/aggregate",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun groupAggregate(input: GroupAggregateQuery<OtpSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Map<String, Double?> = fetch(
            url = "$httpUrl/auth/proof/otp/secrets/group-aggregate",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
    }
    class LiveOneTimePasswordProofApi(val httpUrl: String, val socketUrl: String): Api.OneTimePasswordProofApi {
        override suspend fun establishOneTimePassword(input: EstablishOtp, anyAccessToken: suspend () -> String, masquerade: String?): String = fetch(
            url = "$httpUrl/auth/proof/otp/establish",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun proveOTP(input: IdentificationAndPassword): Proof = fetch(
            url = "$httpUrl/auth/proof/otp/prove",
            method = HttpMethod.POST,
            body = input
        )
    }
    class LivePasswordSecretApi(val httpUrl: String, val socketUrl: String): Api.PasswordSecretApi {
        override suspend fun default(anyAccessToken: suspend () -> String, masquerade: String?): PasswordSecret = fetch(
            url = "$httpUrl/auth/proof/password/secrets/_default_",
            method = HttpMethod.GET,
            token = anyAccessToken,
            masquerade = masquerade,
        )
        override suspend fun permissions(anyAccessToken: suspend () -> String, masquerade: String?): ModelPermissions<PasswordSecret> = fetch(
            url = "$httpUrl/auth/proof/password/secrets/_permissions_",
            method = HttpMethod.GET,
            token = anyAccessToken,
            masquerade = masquerade,
        )
        override suspend fun query(input: Query<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<PasswordSecret> = fetch(
            url = "$httpUrl/auth/proof/password/secrets/query",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun queryPartial(input: QueryPartial<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<Partial<PasswordSecret>> = fetch(
            url = "$httpUrl/auth/proof/password/secrets/query-partial",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun detail(id: UUID, anyAccessToken: suspend () -> String, masquerade: String?): PasswordSecret = fetch(
            url = "$httpUrl/auth/proof/password/secrets/${id.urlify()}",
            method = HttpMethod.GET,
            token = anyAccessToken,
            masquerade = masquerade,
        )
        override suspend fun insertBulk(input: List<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<PasswordSecret> = fetch(
            url = "$httpUrl/auth/proof/password/secrets/bulk",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun insert(input: PasswordSecret, anyAccessToken: suspend () -> String, masquerade: String?): PasswordSecret = fetch(
            url = "$httpUrl/auth/proof/password/secrets",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun upsert(id: UUID, input: PasswordSecret, anyAccessToken: suspend () -> String, masquerade: String?): PasswordSecret = fetch(
            url = "$httpUrl/auth/proof/password/secrets/${id.urlify()}",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun bulkReplace(input: List<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): List<PasswordSecret> = fetch(
            url = "$httpUrl/auth/proof/password/secrets",
            method = HttpMethod.PUT,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun replace(id: UUID, input: PasswordSecret, anyAccessToken: suspend () -> String, masquerade: String?): PasswordSecret = fetch(
            url = "$httpUrl/auth/proof/password/secrets/${id.urlify()}",
            method = HttpMethod.PUT,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun bulkModify(input: MassModification<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Int = fetch(
            url = "$httpUrl/auth/proof/password/secrets/bulk",
            method = HttpMethod.PATCH,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun modifyWithDiff(id: UUID, input: Modification<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): EntryChange<PasswordSecret> = fetch(
            url = "$httpUrl/auth/proof/password/secrets/${id.urlify()}/delta",
            method = HttpMethod.PATCH,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun modify(id: UUID, input: Modification<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): PasswordSecret = fetch(
            url = "$httpUrl/auth/proof/password/secrets/${id.urlify()}",
            method = HttpMethod.PATCH,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun simplifiedModify(id: UUID, input: Partial<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): PasswordSecret = fetch(
            url = "$httpUrl/auth/proof/password/secrets/${id.urlify()}/simplified",
            method = HttpMethod.PATCH,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun bulkDelete(input: Condition<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Int = fetch(
            url = "$httpUrl/auth/proof/password/secrets/bulk-delete",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun delete(id: UUID, anyAccessToken: suspend () -> String, masquerade: String?): Unit = fetch(
            url = "$httpUrl/auth/proof/password/secrets/${id.urlify()}",
            method = HttpMethod.DELETE,
            token = anyAccessToken,
            masquerade = masquerade,
        )
        override suspend fun count(input: Condition<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Int = fetch(
            url = "$httpUrl/auth/proof/password/secrets/count",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun groupCount(input: GroupCountQuery<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Map<String, Int> = fetch(
            url = "$httpUrl/auth/proof/password/secrets/group-count",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun aggregate(input: AggregateQuery<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Double? = fetch(
            url = "$httpUrl/auth/proof/password/secrets/aggregate",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun groupAggregate(input: GroupAggregateQuery<PasswordSecret>, anyAccessToken: suspend () -> String, masquerade: String?): Map<String, Double?> = fetch(
            url = "$httpUrl/auth/proof/password/secrets/group-aggregate",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
    }
    class LivePasswordProofApi(val httpUrl: String, val socketUrl: String): Api.PasswordProofApi {
        override suspend fun establishPassword(input: EstablishPassword, anyAccessToken: suspend () -> String, masquerade: String?): Unit = fetch(
            url = "$httpUrl/auth/proof/password/establish",
            method = HttpMethod.POST,
            token = anyAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun provePasswordOwnership(input: IdentificationAndPassword): Proof = fetch(
            url = "$httpUrl/auth/proof/password/prove",
            method = HttpMethod.POST,
            body = input
        )
    }
    class LiveUserAuthApi(val httpUrl: String, val socketUrl: String): Api.UserAuthApi {
        override suspend fun logIn(input: List<Proof>): IdAndAuthMethods<UUID> = fetch(
            url = "$httpUrl/auth/user/login",
            method = HttpMethod.POST,
            body = input
        )
        override suspend fun logInV2(input: LogInRequest): IdAndAuthMethods<UUID> = fetch(
            url = "$httpUrl/auth/user/login2",
            method = HttpMethod.POST,
            body = input
        )
        override suspend fun checkProofs(input: List<Proof>): ProofsCheckResult<UUID> = fetch(
            url = "$httpUrl/auth/user/proofs-check",
            method = HttpMethod.POST,
            body = input
        )
        override suspend fun openSession(input: String): String = fetch(
            url = "$httpUrl/auth/user/open-session",
            method = HttpMethod.POST,
            body = input
        )
        override suspend fun createSubSession(input: SubSessionRequest, userAccessToken: suspend () -> String, masquerade: String?): String = fetch(
            url = "$httpUrl/auth/user/sub-session",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun getToken(input: OauthTokenRequest): OauthResponse = fetch(
            url = "$httpUrl/auth/user/token",
            method = HttpMethod.POST,
            body = input
        )
        override suspend fun getTokenSimple(input: String): String = fetch(
            url = "$httpUrl/auth/user/token/simple",
            method = HttpMethod.POST,
            body = input
        )
        override suspend fun getSelf(userAccessToken: suspend () -> String, masquerade: String?): User = fetch(
            url = "$httpUrl/auth/user/self",
            method = HttpMethod.GET,
            token = userAccessToken,
            masquerade = masquerade,
        )
        override suspend fun terminateSession(userAccessToken: suspend () -> String, masquerade: String?): Unit = fetch(
            url = "$httpUrl/auth/user/terminate",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
        )
        override suspend fun terminateOtherSession(sessionId: UUID, userAccessToken: suspend () -> String, masquerade: String?): Unit = fetch(
            url = "$httpUrl/auth/user/${sessionId.urlify()}/terminate",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
        )
    }
    class LiveUserSessionApi(val httpUrl: String, val socketUrl: String): Api.UserSessionApi {
        override suspend fun default(userAccessToken: suspend () -> String, masquerade: String?): Session<User, UUID> = fetch(
            url = "$httpUrl/auth/user/sessions/_default_",
            method = HttpMethod.GET,
            token = userAccessToken,
            masquerade = masquerade,
        )
        override suspend fun permissions(userAccessToken: suspend () -> String, masquerade: String?): ModelPermissions<Session<User, UUID>> = fetch(
            url = "$httpUrl/auth/user/sessions/_permissions_",
            method = HttpMethod.GET,
            token = userAccessToken,
            masquerade = masquerade,
        )
        override suspend fun query(input: Query<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): List<Session<User, UUID>> = fetch(
            url = "$httpUrl/auth/user/sessions/query",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun queryPartial(input: QueryPartial<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): List<Partial<Session<User, UUID>>> = fetch(
            url = "$httpUrl/auth/user/sessions/query-partial",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun detail(id: UUID, userAccessToken: suspend () -> String, masquerade: String?): Session<User, UUID> = fetch(
            url = "$httpUrl/auth/user/sessions/${id.urlify()}",
            method = HttpMethod.GET,
            token = userAccessToken,
            masquerade = masquerade,
        )
        override suspend fun insertBulk(input: List<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): List<Session<User, UUID>> = fetch(
            url = "$httpUrl/auth/user/sessions/bulk",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun insert(input: Session<User, UUID>, userAccessToken: suspend () -> String, masquerade: String?): Session<User, UUID> = fetch(
            url = "$httpUrl/auth/user/sessions",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun upsert(id: UUID, input: Session<User, UUID>, userAccessToken: suspend () -> String, masquerade: String?): Session<User, UUID> = fetch(
            url = "$httpUrl/auth/user/sessions/${id.urlify()}",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun bulkReplace(input: List<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): List<Session<User, UUID>> = fetch(
            url = "$httpUrl/auth/user/sessions",
            method = HttpMethod.PUT,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun replace(id: UUID, input: Session<User, UUID>, userAccessToken: suspend () -> String, masquerade: String?): Session<User, UUID> = fetch(
            url = "$httpUrl/auth/user/sessions/${id.urlify()}",
            method = HttpMethod.PUT,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun bulkModify(input: MassModification<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Int = fetch(
            url = "$httpUrl/auth/user/sessions/bulk",
            method = HttpMethod.PATCH,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun modifyWithDiff(id: UUID, input: Modification<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): EntryChange<Session<User, UUID>> = fetch(
            url = "$httpUrl/auth/user/sessions/${id.urlify()}/delta",
            method = HttpMethod.PATCH,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun modify(id: UUID, input: Modification<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Session<User, UUID> = fetch(
            url = "$httpUrl/auth/user/sessions/${id.urlify()}",
            method = HttpMethod.PATCH,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun simplifiedModify(id: UUID, input: Partial<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Session<User, UUID> = fetch(
            url = "$httpUrl/auth/user/sessions/${id.urlify()}/simplified",
            method = HttpMethod.PATCH,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun bulkDelete(input: Condition<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Int = fetch(
            url = "$httpUrl/auth/user/sessions/bulk-delete",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun delete(id: UUID, userAccessToken: suspend () -> String, masquerade: String?): Unit = fetch(
            url = "$httpUrl/auth/user/sessions/${id.urlify()}",
            method = HttpMethod.DELETE,
            token = userAccessToken,
            masquerade = masquerade,
        )
        override suspend fun count(input: Condition<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Int = fetch(
            url = "$httpUrl/auth/user/sessions/count",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun groupCount(input: GroupCountQuery<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Map<String, Int> = fetch(
            url = "$httpUrl/auth/user/sessions/group-count",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun aggregate(input: AggregateQuery<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Double? = fetch(
            url = "$httpUrl/auth/user/sessions/aggregate",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
        override suspend fun groupAggregate(input: GroupAggregateQuery<Session<User, UUID>>, userAccessToken: suspend () -> String, masquerade: String?): Map<String, Double?> = fetch(
            url = "$httpUrl/auth/user/sessions/group-aggregate",
            method = HttpMethod.POST,
            token = userAccessToken,
            masquerade = masquerade,
            body = input
        )
    }
}

