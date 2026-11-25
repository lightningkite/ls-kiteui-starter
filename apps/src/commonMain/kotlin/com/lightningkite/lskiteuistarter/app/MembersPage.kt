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

@Routable("/members")
class MembersPage : Page {
    override val title: Reactive<String> get() = Constant("Members")

    override fun ElementWriter.CanAddTheme.render() {
        reactive {
            if (currentSession() == null) context.pageNavigator.reset(LandingPage())
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
