package com.lightningkite.lskiteuistarter

import com.lightningkite.EmailAddress
import com.lightningkite.lightningserver.*
import com.lightningkite.lightningserver.auth.*
import com.lightningkite.lightningserver.definition.*
import com.lightningkite.lightningserver.definition.builder.*
import com.lightningkite.lightningserver.deprecations.*
import com.lightningkite.lightningserver.encryption.*
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.pathing.*
import com.lightningkite.lightningserver.runtime.*
import com.lightningkite.lightningserver.serialization.*
import com.lightningkite.lightningserver.sessions.*
import com.lightningkite.lightningserver.sessions.proofs.BackupCodeEndpoints
import com.lightningkite.lightningserver.sessions.proofs.DirectProofMethod
import com.lightningkite.lightningserver.sessions.proofs.EmailProofEndpoints
import com.lightningkite.lightningserver.sessions.proofs.FinishProof
import com.lightningkite.lightningserver.sessions.proofs.IdentificationAndPassword
import com.lightningkite.lightningserver.sessions.proofs.PasswordProofEndpoints
import com.lightningkite.lightningserver.sessions.proofs.PinHandler
import com.lightningkite.lightningserver.sessions.proofs.Proof
import com.lightningkite.lightningserver.sessions.proofs.StartedProofMethod
import com.lightningkite.lightningserver.sessions.proofs.TimeBasedOTPProofEndpoints
import com.lightningkite.lightningserver.sessions.proofs.code
import com.lightningkite.lightningserver.sessions.proofs.extensions.constrainAttemptRate
import com.lightningkite.lightningserver.sessions.proofs.proofMethods
import com.lightningkite.lightningserver.settings.*
import com.lightningkite.lightningserver.typed.*
import com.lightningkite.lightningserver.typed.invoke
import com.lightningkite.lightningserver.typed.sdk.module
import com.lightningkite.lightningserver.websockets.*
import com.lightningkite.services.cache.*
import com.lightningkite.services.data.*
import com.lightningkite.services.database.*
import com.lightningkite.services.database.email
import com.lightningkite.services.database.table
import com.lightningkite.services.email.*
import com.lightningkite.services.files.*
import com.lightningkite.services.notifications.*
import com.lightningkite.services.sms.*
import com.lightningkite.toEmailAddress
import io.ktor.http.decodeURLPart
import io.ktor.http.encodeURLPathPart
import kotlin.uuid.Uuid
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.collections.plus
import kotlin.text.compareTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.toString


object UserAuth : PrincipalType<User, Uuid>, ServerBuilder() {
    // principal fields

    override val subjectSerializer: KSerializer<User> = User.serializer()
    override val idSerializer: KSerializer<Uuid> = Uuid.serializer()

    context(server: ServerRuntime)
    override suspend fun fetch(id: Uuid): User = UserEndpoints.info.table().get(id) ?: throw NotFoundException()

    context(server: ServerRuntime)
    override suspend fun fetchByProperty(property: String, value: String): User? = when (property) {
        "email" -> UserEndpoints.info.table()
            .run {
                findOne(condition { it.email eq value.toEmailAddress() }) ?: insertOne(User(email = value.toEmailAddress()))
            }

        else -> super.fetchByProperty(property, value)
    }

    override val precache: List<AuthCacheKey<User, *>> = listOf(RoleCache)


    // caching

    object RoleCache : AuthCacheKey<User, UserRole> {
        override val id: String = "role"
        override val serializer: KSerializer<UserRole> = kotlinx.serialization.serializer()
        override val expireAfter: Duration = 5.minutes

        context(_: ServerRuntime)
        override suspend fun calculate(input: Authentication<User>): UserRole = input.fetch().role ?: UserRole.NoOne

        context(_: ServerRuntime) suspend fun Authentication<User>.userRole() = get(RoleCache)
        context(_: ServerRuntime) suspend fun AuthAccess<User>.userRole() = auth.userRole()
    }

    val authEndpoints = path.path("user") include SessionEndpoints

    private val proofs = path.path("proof")

    val pins = PinHandler(Server.cache, "pins")

    val email = proofs.path("email") module EmailEndpoints(pins)
    val totp = proofs.path("totp") module TimeBasedOTPProofEndpoints(Server.database, Server.cache)
    val password = proofs.path("password") module PasswordProofEndpoints(Server.database, Server.cache)
    val backupCodes = proofs.path("backup-codes") module BackupCodeEndpoints(Server.database, Server.cache)

    class EmailEndpoints(val pins: PinHandler) : ServerBuilder() {
        val proof = path include EmailProofEndpoints(
            pin = pins,
            email = Server.email,
            emailTemplate = { to, pin ->
                val name = Server.users.info.table().findOne(condition { it.email eq to.toEmailAddress() })?.name
                Email(
                    subject = "Log In Code",
                    to = listOf(EmailAddressWithName(to)),
                    html = createHTML(true).html {
                        emailBase {
                            header("Log In Code")
                            paragraph(
                                buildString {
                                    if (name != null) appendLine("Hi $name,")
                                    append("Your log in code is:")
                                }
                            )
                            code(pin)
                            paragraph("If you did not request this code, you can safely ignore this email.")
                        }
                    }
                )
            }
        )

        val verifyNewEmail = path.path("verify-new-email").post bind ApiHttpHandler(
            summary = "Verify New Email",
            description = "Sends a verification passcode to a new email.",
            auth = UserAuth.require(),
            implementation = { newEmail: EmailAddress ->
                val self = auth.fetch()

                pins.cache().constrainAttemptRate("email-pin-count-${newEmail}") {
                    val p = pins.establish(newEmail.raw)

                    Server.email().send(
                        Email(
                            subject = "New Email Verification",
                            to = listOf(EmailAddressWithName(newEmail, self.name)),
                            html = createHTML(true).html {
                                emailBase {
                                    header("New Email Verification")
                                    paragraph("Here is your verification passcode,")
                                    code(p.pin)
                                    paragraph("If you did not request this code, you can safely ignore this email.")
                                }
                            }
                        )
                    )

                    p.key
                }
            }
        )
    }

    object SessionEndpoints : AuthEndpoints<User, Uuid>(
        principal = UserAuth,
        database = Server.database,
    ) {
        context(server: ServerRuntime)
        override suspend fun requiredProofStrengthFor(subject: User): Int {
            // AppStoreTester
            if (subject._id.toString() == "f00ffbaa-abf9-497d-a75a-442f1c77c1e9") return 10

            val methods = server.proofMethods
                .filter { it.established(UserAuth, subject) }
                .filter { it.info.via != backupCodes.info.via }

            return if (methods.size > 1) 20 else 10
        }

        context(server: ServerRuntime)
        override suspend fun sessionExpiration(subject: User): Instant? = null

        context(server: ServerRuntime)
        override suspend fun sessionStaleAfter(subject: User): Duration? = null
    }
}
