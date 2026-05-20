package com.heroscript.sdk

import com.heroscript.*
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.and
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import com.lightningkite.services.database.neq

class UserSession(
    val api: Api,
    val userId: User.ID,
) : CachedApi(api) {
    val self: Reactive<User> = rememberSuspending { api.userAuth.getSelf() }
    val activeMemberships: Reactive<List<ClinicMembership>> = rememberSuspending {
        api.clinicMembership.query(
            Query(condition {
                (it.user eq userId) and
                (it.acceptedAt neq null) and
                (it.deactivatedAt eq null)
            })
        )
    }
}
