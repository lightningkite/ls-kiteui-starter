package com.lightningkite.lskiteuistarter.sdk

import com.lightningkite.lskiteuistarter.User

class UserSession(
    val api: Api,
    val userId: User.ID,
) : CachedApi(api) {

}
