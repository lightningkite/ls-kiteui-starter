package com.lightningkite.template

import com.lightningkite.lightningserver.auth.Authentication
import com.lightningkite.lightningserver.auth.authRequired
import com.lightningkite.lightningserver.auth.user
import com.lightningkite.lightningserver.cache.CacheSettings
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.core.ServerPathGroup
import com.lightningkite.lightningserver.db.DatabaseSettings
import com.lightningkite.lightningserver.email.EmailSettings
import com.lightningkite.lightningserver.http.HttpResponse
import com.lightningkite.lightningserver.http.handler
import com.lightningkite.lightningserver.meta.metaEndpoints
import com.lightningkite.lightningserver.settings.setting
import com.lightningkite.prepareModelsServerCore

object Server: ServerPathGroup(ServerPath.root) {

    val cache = setting("cache", CacheSettings())
    val database = setting("database", DatabaseSettings())
    val email = setting("email", EmailSettings())

    init{
        RoleCacheKey

        prepareModelsShared()
        prepareModelsServerCore()
        com.lightningkite.prepareModelsShared()

        Authentication.isDeveloper = authRequired<User> {
            it.role() >= UserRole.Developer
        }
        Authentication.isSuperUser = authRequired<User> {
            it.role() >= UserRole.Root
        }
        Authentication.isAdmin = authRequired<User> {
            it.role() >= UserRole.Admin
        }
    }

    val root = get.handler {
        if (it.user<User?>() == null) HttpResponse.redirectToGet(auth.userAuth.html.html0.path.toString())
        else HttpResponse.redirectToGet(meta.admin.path.toString() + "models/auction")
    }

    val users = UserEndpoints(path("users"))
    val auth = AuthenticationEndpoints(path("auth"))


    val meta = path("meta").metaEndpoints()
}