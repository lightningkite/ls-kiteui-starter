package com.lightningkite.lskiteuistarter

import com.lightningkite.lskiteuistarter.api.*
import com.lightningkite.lskiteuistarter.app.*
import com.lightningkite.lskiteuistarter.app.views.*

import com.lightningkite.lightningserver.runtime.test.execute
import com.lightningkite.lightningserver.sessions.EstablishPassword
import com.lightningkite.lskiteuistarter.app.views.SecurityPage
import com.lightningkite.services.database.Condition
import com.lightningkite.services.email.TestEmailService
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals

class SecurityTest {
    @Test
    fun reAuthenticateThenChangePasswordAndStartAuthenticatorSetup() = integrationTest(
        initialPage = SecurityPage(),
        setup = {
            val user = createUser()
            UserAuth.password.establish(UserAuth, user._id, EstablishPassword("correct horse battery"))
            loginAs(user)
        },
    ) { server ->
        // With a password set, UserAuth requires two proofs: password and an emailed code.
        waitForText("Confirm it's you")
        click(findClickable("Enter Password").single().path)
        waitForId("passwordInput")
        setValue("passwordInput", "correct horse battery")
        click("submitButton")

        waitFor(description = "email option") { findClickable("Email Code").isNotEmpty() && "passwordInput" !in interactiveSnapshot() }
        click(findClickable("Email Code").single().path)
        waitForId("codeInput")
        val email = server.execute { (Server.email() as TestEmailService).lastEmailTo("test@example.com") }!!
        val pin = Regex(""">\s*([A-Z]{6})\s*<""").find(email.html)!!.groupValues[1]
        setValue("codeInput", pin)
        click("submitButton")
        waitForId("loginButton")
        click("loginButton")
        waitForText("Two-Factor Authentication", timeoutMs = 15_000)

        val (newPassword, confirmation, deviceName) = findAll("TextInput").map { it.path }
        setValue(newPassword, "a brand new password")
        setValue(confirmation, "a brand new password")
        click(findClickable("Set Password").single().path)
        waitForText("Password saved")
        val activePasswords = server.execute {
            UserAuth.password.modelInfo.table().find(Condition.Always).toList().filter { it.disabledAt == null }
        }
        assertEquals(1, activePasswords.size, "The old password is disabled when a new one is set")

        setValue(deviceName, "My phone")
        click(findClickable("Add Authenticator App").single().path)
        waitForText("Scan this with your authenticator app")
    }
}
