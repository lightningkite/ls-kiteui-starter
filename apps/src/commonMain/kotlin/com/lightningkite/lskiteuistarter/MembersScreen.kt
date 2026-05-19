// by Claude — Members management screen
package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.lskiteuistarter.sdk.*
import com.lightningkite.lskiteuistarter.views.LandingPage
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.*
import com.lightningkite.services.database.*

@Routable("/members")
class MembersPage : Page {
    override val title: Reactive<String> get() = Constant("Members")

    override fun ElementWriter.CanAddTheme.render() {
        reactive {
            if (currentSession() == null)
                context.pageNavigator.reset(LandingPage())
        }

        val session = currentSessionNotNull

        val memberships = remember {
            val s = session()
            s.memberships.list(Query(Condition.Always))()
        }

        scrolling.col {
            centered.h2("Members")

            col {
                forEachById(memberships, id = { it._id }) { membershipReactive ->
                    card.row {
                        expanding.col {
                            text { ::content {
                                val m = membershipReactive()
                                val user = session().users[m.user]()
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
                                val org = session().organizations[m.organization]()
                                "Org: ${org?.name ?: m.organization.toString()}"
                            } }
                        }
                    }
                }
            }
        }
    }
}
