// by Claude — Members management screen
package com.lightningkite.lskiteuistarter.app

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.lskiteuistarter.app.sdk.*
import com.lightningkite.lskiteuistarter.app.views.LandingPage
import com.lightningkite.lskiteuistarter.user
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.services.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Routable("/members")
class MembersPage : Page {
    override val title: Reactive<String> get() = Constant("Members")

    override fun ElementWriter.CanAddTheme.render() {
        // Resetting the nav stack can't happen synchronously here, on the page's own initial
        // render: the swapView backing navigatorView hasn't finished subscribing to
        // navigator.currentPage yet at that exact moment, so a same-tick reset() lands in the
        // stack (stack.value updates) but never reaches the view -- the page silently hangs on
        // this screen instead of swapping to LandingPage. Confirmed empirically: a bare
        // `launch { reset(...) }` (no delay) reproduces the hang every time; a real delay lets
        // the swapView's own setup finish first, exactly like LandingPage's redirect already
        // works reliably because its currentSession.await() naturally takes longer than that
        // window. 50ms is a deliberate, generous margin verified stable across repeated runs
        // under load -- see integration-tests SmokeTest.loggedOutUserRedirectedFromHome.
        reactive {
            if (currentSession() == null)
                launch { delay(50); context.pageNavigator.reset(LandingPage()) }
        }

        val session = currentSessionNotNull

        val search = Signal("")
        val matchingUsers = rememberSuspending {
            val query = search().trim()
            if (query.isEmpty()) return@rememberSuspending null
            delay(300) // Typing restarts this block, so this debounces the search.
            session().api.user.textQuery(TextIndexQuery(query)).map { it.model._id }
        }
        val memberships = remember {
            val users = matchingUsers()
            val s = session()
            s.memberships.list(Query(if (users == null) Condition.Always else Condition { it.user inside users }))()
        }

        scrolling.col {
            centered.h2("Members")

            textInput {
                hint = "Search by name or email"
                content bind search
            }

            renderListIn(ElementWriter::col, memberships, id = { it._id }) { membershipReactive ->
                card.row {
                    expanding.col {
                        text { ::content {
                            val m = membershipReactive()
                            val user = session().users.item(m.user)()
                            "User: ${user?.name?.takeIf { it.isNotBlank() } ?: user?.email?.raw ?: m.user.toString()}"
                        } }
                        subtext { ::content {
                            val m = membershipReactive()
                            buildString {
                                append("Role: ${m.role.name}")
                                if (m.deactivatedAt != null) append(" (Deactivated)")
                            }
                        } }
                    }
                    col {
                        subtext { ::content {
                            val m = membershipReactive()
                            val org = session().organizations.item(m.organization)()
                            "Org: ${org?.name ?: m.organization.toString()}"
                        } }
                    }
                }
            }
        }
    }
}
