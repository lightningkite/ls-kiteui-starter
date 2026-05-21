package com.heroscript

import com.lightningkite.kiteui.exceptions.installLsError
import com.lightningkite.kiteui.exceptions.installSmartHandlers
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.appNav
import com.lightningkite.kiteui.views.l2.appNavFactory
import com.lightningkite.kiteui.views.l2.appNavHamburger
import com.lightningkite.kiteui.views.l2.appNavTopAndLeft
import com.heroscript.sdk.UserSession
import com.heroscript.sdk.currentSession
import com.heroscript.sdk.installLoggedOutErrors
import com.heroscript.sdk.sessionToken
import com.heroscript.utils.fcmSetup
import com.heroscript.utils.notificationPermissions
import com.heroscript.utils.requestNotificationPermissions
import com.heroscript.views.DashboardPage
import com.heroscript.views.LoginPage
import com.heroscript.views.catalog.CatalogListPage
import com.heroscript.views.checkAppVersion
import com.heroscript.views.clinics.ClinicListPage
import com.heroscript.views.clinics.ClinicSettingsPage
import com.heroscript.views.invoices.InvoiceListPage
import com.heroscript.views.ops.OrderMonitorPage
import com.heroscript.views.orders.OrdersListPage
import com.heroscript.views.patients.PatientListPage
import com.heroscript.views.pharmacies.PharmacyListPage
import com.heroscript.views.profile.ProfilePage
import com.heroscript.views.refills.RefillQueuePage
import com.heroscript.views.users.DeaQueuePage
import com.heroscript.views.users.UserListPage
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.context.reactiveSuspending
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember

val defaultTheme = Theme.flat2("default", Angle(0.55f))
val appTheme = Signal(defaultTheme)

// Notification Items
val fcmToken: Signal<String?> = Signal(null)
val setFcmToken = { token: String -> fcmToken.value = token } // This is for iOS. It is used in the iOS app. Do not remove.

/** The clinic the user is currently acting in. Null when the user has no clinic memberships. */
val activeClinic: Signal<Clinic.ID?> = Signal(null)

/** True when the logged-in user has at least one active clinic membership. */
private fun com.lightningkite.reactive.context.ReactiveContext.hasClinicContext(): Boolean {
    val session = currentSession() ?: return false
    return session.activeMemberships().isNotEmpty()
}

/** True when the logged-in user's system role is Admin or higher (Ops). */
private fun com.lightningkite.reactive.context.ReactiveContext.hasOpsContext(): Boolean {
    val session = currentSession() ?: return false
    return session.self().role >= UserRole.Admin
}

fun ViewWriter.app(navigator: PageNavigator, dialog: PageNavigator) {
    context.exceptionHandlers.installSmartHandlers()
    context.exceptionHandlers.installLsError()
    context.exceptionHandlers.installLoggedOutErrors()

    AppScope.reactiveSuspending {
        if (currentSession() == null) return@reactiveSuspending
        val permission = notificationPermissions()
        when (permission) {
            false -> {}
            true -> fcmSetup()
            null -> {
                context.confirmDanger(
                    "Send notifications?",
                    "HeroScript would like to send you notifications.",
                    "Allow"
                ) {
                    requestNotificationPermissions()
                }
            }
        }
    }

    AppScope.reactiveSuspending {
        val memberships = currentSession()?.activeMemberships() ?: emptyList()
        if (activeClinic.value != null && memberships.none { it.clinic == activeClinic.value }) {
            activeClinic.value = null
        }
        if (activeClinic.value == null) {
            activeClinic.value = memberships.firstOrNull()?.clinic
        }
    }

    checkAppVersion()

    val isWide = remember { AppState.windowInfo().width > 60.rem }
    reactive {
        context.appNavFactory.value = if (isWide()) ViewWriter::appNavTopAndLeft else ViewWriter::appNavHamburger
    }

    col {
        gap = 0.px
        clinicSwitcherChip(navigator)
        expanding.frame {
            appNav(navigator, dialog) {
                val appNavRef = this
                appName = "HeroScript"

                // Role-aware nav. Each item's `hidden` lambda is reactive (re-runs
                // when the session, role, or memberships change), so the list itself
                // can stay static and we avoid the `::navItems { ... }` lifecycle issue
                // that bit a prior attempt. The two NavGroups hide as a unit when the
                // user lacks the corresponding context.
                appNavRef.navItems = listOf(
                    NavGroup(
                        title = { "Clinic" },
                        icon = { Icon.home },
                        hidden = { !hasClinicContext() },
                        children = {
                            listOf(
                                NavLink(title = { "Dashboard" }, icon = { Icon.home }, destination = { { DashboardPage() } }),
                                NavLink(title = { "Orders" }, icon = { Icon.list }, destination = { { OrdersListPage() } }),
                                NavLink(title = { "Refill Queue" }, icon = { Icon.sync }, destination = { { RefillQueuePage() } }),
                                NavLink(title = { "Patients" }, icon = { Icon.person }, destination = { { PatientListPage() } }),
                                NavLink(title = { "Catalog" }, icon = { Icon.list }, destination = { { CatalogListPage() } }),
                                NavLink(title = { "Clinic Settings" }, icon = { Icon.settings }, destination = { { ClinicSettingsPage() } }),
                                NavLink(title = { "Invoices" }, icon = { Icon.list }, destination = { { InvoiceListPage() } }),
                            )
                        }
                    ),
                    NavGroup(
                        title = { "Ops" },
                        icon = { Icon.settings },
                        hidden = { !hasOpsContext() },
                        children = {
                            listOf(
                                NavLink(title = { "Order Monitor" }, icon = { Icon.list }, destination = { { OrderMonitorPage() } }),
                                NavLink(title = { "Clinics" }, icon = { Icon.group }, destination = { { ClinicListPage() } }),
                                NavLink(title = { "Users" }, icon = { Icon.person }, destination = { { UserListPage() } }),
                                NavLink(title = { "Catalog" }, icon = { Icon.list }, destination = { { CatalogListPage() } }),
                                NavLink(title = { "Pharmacies" }, icon = { Icon.group }, destination = { { PharmacyListPage() } }),
                                NavLink(title = { "DEA Queue" }, icon = { Icon.certification }, destination = { { DeaQueuePage() } }),
                                NavLink(title = { "Invoices" }, icon = { Icon.list }, destination = { { InvoiceListPage() } }),
                            )
                        }
                    ),
                    NavLink(title = { "Profile" }, icon = { Icon.person }, destination = { { ProfilePage() } }),
                )

                actions = listOf(
                    NavAction("Sign Out", Icon.logout) {
                        try {
                            currentSession()?.api?.userAuth?.terminateSession()
                        } catch (_: Exception) {
                        } finally {
                            sessionToken set null
                            activeClinic.value = null
                            context.pageNavigator.reset(LoginPage())
                        }
                    }
                )

                reactive {
                    appNavRef.exists = navigator.currentPage() !is FullscreenPage
                }
            }
        }
    }
}

private fun ViewWriter.clinicSwitcherChip(navigator: PageNavigator) {
    shownWhen {
        val memberships = currentSession()?.activeMemberships() ?: emptyList()
        memberships.size > 1 && navigator.currentPage() !is FullscreenPage
    }.bar.row {
        space()
        card.menuButton {
            row {
                centered.text {
                    ::content {
                        val session = currentSession()
                        val activeId = activeClinic()
                        val name = when {
                            session == null -> "Clinic"
                            activeId == null -> "Select clinic"
                            else -> session.clinics[activeId]()?.name ?: "Clinic"
                        }
                        "Clinic: $name"
                    }
                }
                centered.icon(Icon.menu, "Switch clinic")
            }
            opensMenu {
                col {
                    reactive {
                        clearChildren()
                        val session = currentSession() ?: return@reactive
                        val memberships = session.activeMemberships()
                        memberships.forEach { m ->
                            button {
                                row {
                                    centered.text {
                                        ::content {
                                            session.clinics[m.clinic]()?.name ?: m.clinic.toString()
                                        }
                                    }
                                    centered.shownWhen { activeClinic() == m.clinic }.subtext("· active")
                                }
                                onClick {
                                    activeClinic.value = m.clinic
                                    context.closePopovers()
                                }
                            }
                        }
                    }
                }
            }
        }
        space()
    }
}

interface FullscreenPage
