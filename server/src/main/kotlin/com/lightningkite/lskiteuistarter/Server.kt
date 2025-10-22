package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.*
import com.lightningkite.lightningserver.auth.*
import com.lightningkite.lightningserver.cors.CorsInterceptor
import com.lightningkite.lightningserver.cors.CorsSettings
import com.lightningkite.lightningserver.definition.*
import com.lightningkite.lightningserver.definition.builder.*
import com.lightningkite.lightningserver.files.FileSystemEndpoints
import com.lightningkite.lightningserver.files.UploadEarlyEndpoint
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.serialization.*
import com.lightningkite.lightningserver.typed.*
import com.lightningkite.lightningserver.typed.sdk.module
import com.lightningkite.lightningserver.websockets.*
import com.lightningkite.services.cache.*
import com.lightningkite.services.cache.dynamodb.DynamoDbCache
import com.lightningkite.services.database.*
import com.lightningkite.services.database.jsonfile.JsonFileDatabase
import com.lightningkite.services.database.mongodb.MongoDatabase
import com.lightningkite.services.email.*
import com.lightningkite.services.email.javasmtp.JavaSmtpEmailService
import com.lightningkite.services.files.*
import com.lightningkite.services.files.s3.S3PublicFileSystem
import com.lightningkite.services.notifications.*
import com.lightningkite.services.notifications.fcm.FcmNotificationClient
import com.lightningkite.lskiteuistarter.UserAuth.RoleCache.userRole

object Server: ServerBuilder() {

    // Settings
    val cache = setting("cache", Cache.Settings())
    val database = setting("database", Database.Settings())
    val email = setting("email", EmailService.Settings())
    val notifications = setting("notifications", default = NotificationService.Settings("console"))
    val webUrl = setting("webUrl", "http://localhost:8080")
    val cors = setting("cors", CorsSettings())
    val files = setting("files", PublicFileSystem.Settings())

    // this seems like boilerplate to me. What does it do?
    init {
        install(CorsInterceptor(cors))
        registerBasicMediaTypeCoders()

        MongoDatabase
        JsonFileDatabase
        FcmNotificationClient
        JavaSmtpEmailService
        S3PublicFileSystem
        DynamoDbCache

        // what is this line doing?
        AuthRequirement.isSuperUser = UserAuth.require { it.userRole() >= UserRole.Root }
    }

    // Endpoints, tasks, and schedules

    val root = path.get bind HttpHandler {
        HttpResponse.plainText("Welcome to Lightning Server!")
    }
    val uploadEarly = path.path("upload-early") module UploadEarlyEndpoint(files = files, database = database, fileScanner = Runtime.Constant(emptyList()))
    val localFileServer = path.path("files") include FileSystemEndpoints(files)

    val example = path.path("example-endpoint").get bind ApiHttpHandler(
        summary = "Example Endpoint",
        auth = noAuth,
        implementation = { _: Unit -> 42 }
    )
    val example2 = path.path("example-endpoint").post bind ApiHttpHandler(
        summary = "Example Endpoint",
        auth = UserAuth.require(),
        implementation = { number: Int -> number + 42 }
    )

    val users = path.path("users") module UserEndpoints
    val authEndpoints = path.path("auth") module UserAuth
    val fcmTokens = path.path("fcmTokens") module FcmTokenEndpoints

    val multiplex = path.path("multiplex") bind MultiplexWebSocketHandler()
    val base = path bind QueryParamWebSocketHandler()
    val meta = path.path("meta") module MetaEndpoints("com.lightningkite.lskiteuistarter",
        database,
        cache
    )
}