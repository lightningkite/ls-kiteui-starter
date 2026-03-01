// by Claude — Members management screen
package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.lskiteuistarter.sdk.*
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.*
import com.lightningkite.services.database.*

@Routable("/members")
class MembersPage : Page {
    override val title: Reactive<String> get() = Constant("Members")

    override fun ViewWriter.render() {
        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
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
                            text { ::content { "User: ${membershipReactive().user}" } }
                            subtext { ::content {
                                val m = membershipReactive()
                                buildString {
                                    append("Role: ${m.role.name}")
                                    if (m.deactivatedAt != null) append(" (Deactivated)")
                                }
                            } }
                        }
                        col {
                            subtext { ::content { "Org: ${membershipReactive().organization}" } }
                        }
                    }
                }
            }
        }
    }
}
