package com.lightningkite.template.sdk

import com.lightningkite.*
import com.lightningkite.lightningdb.*
import com.lightningkite.kiteui.*
import kotlinx.datetime.*
import com.lightningkite.serialization.*
import com.lightningkite.lightningserver.db.*
import com.lightningkite.lightningserver.auth.*

interface Api2 {
fun withHeaderCalculator(headerCalculator: suspend () -> List<Pair<String, String>>): Api2
suspend fun exampleEndpoint(): kotlin.Int
suspend fun getServerHealth(): com.lightningkite.lightningserver.serverhealth.ServerHealth
suspend fun bulkRequest(input: Map<String, com.lightningkite.lightningserver.typed.BulkRequest>): Map<String, com.lightningkite.lightningserver.typed.BulkResponse>
interface Api2EmailProof : EmailProofClientEndpoints{
}
val emailProof: Api2EmailProof
interface Api2FunnelInstance : ClientModelRestEndpoints<com.lightningkite.lightningserver.monitoring.FunnelInstance, com.lightningkite.UUID>{
suspend fun simplifiedModify(id: com.lightningkite.UUID, input: com.lightningkite.serialization.Partial<com.lightningkite.lightningserver.monitoring.FunnelInstance>): com.lightningkite.lightningserver.monitoring.FunnelInstance
suspend fun getFunnelHealth(date: kotlinx.datetime.LocalDate): List<com.lightningkite.lightningserver.monitoring.FunnelSummary>
suspend fun summarizeFunnelsNow(input: kotlinx.datetime.LocalDate): kotlin.Unit
suspend fun startFunnelInstance(input: com.lightningkite.lightningserver.monitoring.FunnelStart): com.lightningkite.UUID
suspend fun errorFunnelInstance(id: com.lightningkite.UUID, input: kotlin.String): kotlin.Unit
suspend fun setStepFunnelInstance(id: com.lightningkite.UUID, input: kotlin.Int): kotlin.Unit
suspend fun successFunnelInstance(id: com.lightningkite.UUID): kotlin.Unit
}
val funnelInstance: Api2FunnelInstance
interface Api2FunnelSummary : ClientModelRestEndpoints<com.lightningkite.lightningserver.monitoring.FunnelSummary, com.lightningkite.UUID>{
suspend fun simplifiedModify(id: com.lightningkite.UUID, input: com.lightningkite.serialization.Partial<com.lightningkite.lightningserver.monitoring.FunnelSummary>): com.lightningkite.lightningserver.monitoring.FunnelSummary
}
val funnelSummary: Api2FunnelSummary
interface Api2OneTimePasswordProof : AuthenticatedOneTimePasswordProofClientEndpoints, OneTimePasswordProofClientEndpoints{
}
val oneTimePasswordProof: Api2OneTimePasswordProof
interface Api2OtpSecret : ClientModelRestEndpoints<com.lightningkite.lightningserver.auth.proof.OtpSecret, com.lightningkite.UUID>{
suspend fun simplifiedModify(id: com.lightningkite.UUID, input: com.lightningkite.serialization.Partial<com.lightningkite.lightningserver.auth.proof.OtpSecret>): com.lightningkite.lightningserver.auth.proof.OtpSecret
}
val otpSecret: Api2OtpSecret
interface Api2PasswordProof : AuthenticatedPasswordProofClientEndpoints, PasswordProofClientEndpoints{
}
val passwordProof: Api2PasswordProof
interface Api2PasswordSecret : ClientModelRestEndpoints<com.lightningkite.lightningserver.auth.proof.PasswordSecret, com.lightningkite.UUID>{
suspend fun simplifiedModify(id: com.lightningkite.UUID, input: com.lightningkite.serialization.Partial<com.lightningkite.lightningserver.auth.proof.PasswordSecret>): com.lightningkite.lightningserver.auth.proof.PasswordSecret
}
val passwordSecret: Api2PasswordSecret
interface Api2User : ClientModelRestEndpoints<com.lightningkite.template.User, com.lightningkite.UUID>{
suspend fun simplifiedModify(id: com.lightningkite.UUID, input: com.lightningkite.serialization.Partial<com.lightningkite.template.User>): com.lightningkite.template.User
}
val user: Api2User
interface Api2UserAuth : UserAuthClientEndpoints<com.lightningkite.UUID>, AuthenticatedUserAuthClientEndpoints<com.lightningkite.template.User, com.lightningkite.UUID>{
}
val userAuth: Api2UserAuth
interface Api2UserSession : ClientModelRestEndpoints<com.lightningkite.lightningserver.auth.subject.Session<com.lightningkite.template.User, com.lightningkite.UUID>, com.lightningkite.UUID>{
suspend fun simplifiedModify(id: com.lightningkite.UUID, input: com.lightningkite.serialization.Partial<com.lightningkite.lightningserver.auth.subject.Session<com.lightningkite.template.User, com.lightningkite.UUID>>): com.lightningkite.lightningserver.auth.subject.Session<com.lightningkite.template.User, com.lightningkite.UUID>
}
val userSession: Api2UserSession
}
