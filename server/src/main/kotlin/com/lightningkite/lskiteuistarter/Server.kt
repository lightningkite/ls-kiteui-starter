package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.auth.*
import com.lightningkite.lightningserver.cors.CorsInterceptor
import com.lightningkite.lightningserver.cors.CorsSettings
import com.lightningkite.lightningserver.definition.Runtime
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.files.FileSystemEndpoints
import com.lightningkite.lightningserver.files.Files
import com.lightningkite.lightningserver.files.UploadEarlyEndpoint
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.media.Media
import com.lightningkite.lightningserver.plainText
import com.lightningkite.lightningserver.serialization.StandardWithExternalModule
import com.lightningkite.lightningserver.serialization.registerBasicMediaTypeCoders
import com.lightningkite.lightningserver.typed.MetaEndpoints
import com.lightningkite.lightningserver.typed.sdk.module
import com.lightningkite.lightningserver.websockets.MultiplexWebSocketHandler
import com.lightningkite.lightningserver.websockets.QueryParamWebSocketHandler
import com.lightningkite.lskiteuistarter.UserAuth.RoleCache.userRole
import com.lightningkite.lskiteuistarter.data.*
import com.lightningkite.services.LoggingTelemetryBackend
import com.lightningkite.services.cache.Cache
import com.lightningkite.services.cache.dynamodb.DynamoDbCache
import com.lightningkite.services.data.MaxLength
import com.lightningkite.services.database.Database
import com.lightningkite.services.database.jsonfile.JsonFileDatabase
import com.lightningkite.services.database.mongodb.MongoDatabase
import com.lightningkite.services.database.validation.AnnotationValidators
import com.lightningkite.services.email.EmailService
import com.lightningkite.services.email.javasmtp.JavaSmtpEmailService
import com.lightningkite.services.files.ExternalFileSystem
import com.lightningkite.services.files.s3.S3ExternalFileSystem
import com.lightningkite.services.notifications.NotificationService
import com.lightningkite.services.notifications.fcm.FcmNotificationClient
import com.lightningkite.services.otel.OtelTelemetryBackend

object Server : ServerBuilder() {
    override val annotationValidators: Runtime<AnnotationValidators> = Runtime.Cached {
        AnnotationValidators.StandardWithExternalModule() + AnnotationValidators.Files() + AnnotationValidators.Media()
    }

    // Settings
    val cache = setting("cache", Cache.Settings())
    val database = setting("database", Database.Settings())
    val email = setting("email", EmailService.Settings())
    val notifications = setting("notifications", default = NotificationService.Settings("console"))
    val webUrl = setting("webUrl", "http://localhost:8080")
    val cors = setting("cors", CorsSettings())
    val files = setting("files", ExternalFileSystem.Settings())

    init {
        val securityHeaders = install(SecurityHeadersInterceptor())
        val accessLog = install(AccessLogInterceptor())
        val corsInterceptor = install(CorsInterceptor(cors))
        registerBasicMediaTypeCoders()

        MongoDatabase
        JsonFileDatabase
        FcmNotificationClient
        JavaSmtpEmailService
        S3ExternalFileSystem
        DynamoDbCache
        LoggingTelemetryBackend
        OtelTelemetryBackend

        AuthRequirement.isSuperUser = UserAuth.require { it.userRole() >= UserRole.Root }
    }

    // Endpoints, tasks, and schedules

    val root = path.get bind HttpHandler {
        HttpResponse.plainText("Welcome to Lightning Server!")
    }
    val uploadEarly = path.path("upload-early") module UploadEarlyEndpoint(
        files = files,
        database = database,
        fileScanner = Runtime.Constant(emptyList())
    )
    val localFileServer = path.path("files") include FileSystemEndpoints(files)

    val appReleases = path.path("app-releases") module AppReleaseEndpoints
    val users = path.path("users") module UserEndpoints
    val authEndpoints = path.path("auth") module UserAuth
    val fcmTokens = path.path("fcmTokens") module FcmTokenEndpoints

    val organizations = path.path("organizations") module OrganizationEndpoints
    val memberships = path.path("memberships") module MembershipEndpoints

    val multiplex = path.path("multiplex") bind MultiplexWebSocketHandler()
    val base = path bind QueryParamWebSocketHandler()
    val meta = path.path("meta") module MetaEndpoints(
        "com.lightningkite.lskiteuistarter",
        database,
        cache
    )
}