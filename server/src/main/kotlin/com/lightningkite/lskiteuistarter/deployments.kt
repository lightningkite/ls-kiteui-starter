package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.cors.CorsSettings
import com.lightningkite.lightningserver.definition.*
import com.lightningkite.lightningserver.engine.awsserverless.AwsAdapter
import com.lightningkite.lightningserver.terraform.*
import com.lightningkite.lightningserver.terraform.awsserverless.*
import com.lightningkite.services.LoggingSettings
import com.lightningkite.services.cache.dynamodb.awsDynamoDb
import com.lightningkite.services.data.snakeCase
import com.lightningkite.services.data.toEmailAddress
import com.lightningkite.services.database.mongodb.mongodbAtlasFree
import com.lightningkite.services.email.javasmtp.awsSesDomainConfiguration
import com.lightningkite.services.email.javasmtp.awsSesSmtp
import com.lightningkite.services.files.s3.awsS3Bucket
import com.lightningkite.services.terraform.*
import io.github.oshai.kotlinlogging.Level
import kotlinx.serialization.json.JsonObject
import software.amazon.awssdk.regions.Region
import java.io.File
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class AwsHandler : AwsAdapter(Server.build())

object LkEnv : TerraformAwsServerlessDomainBuilder<Server>(Server) {
    override val handler: KClass<out AwsAdapter> = AwsHandler::class

    override val displayName = "LS KiteUI Starter"
    override val deploymentTag: String get() = displayName
    override val projectPrefix: String = "ls-kiteui-starter"

    override val domain = "api.lskiteuistarter.cs.lightningkite.com"
    override val domainZone = "cs.lightningkite.com"
    override val terraformRoot: File = File("server/terraform/lk")

    override val timeout: Duration = 5.minutes

    override val storageBucket = "lightningkite-terraform"
    override val storageBucketPath: String
        get() = super.storageBucketPath

    override val debug = true
    override val emergencyContact = "joseph@lightningkite.com".toEmailAddress()

    override val region = Region.US_WEST_2!!
    override val applicationVpc: AwsVpc = AwsVpc.None

    override val secretsSource: SecretSource =
        AwsSecretSource(projectPrefix, idPrefix = displayName.snakeCase(), region)

    override fun Server.settings() {
        require(TerraformProviderImport.mongodbAtlas)
        require(TerraformProvider(TerraformProviderImport.mongodbAtlas, null, JsonObject(emptyMap())))

        database.mongodbAtlasFree(orgId = "6323a65c43d66b56a2ea5aea")
        val config = awsSesDomainConfiguration("email", emergencyContact)
        email.awsSesSmtp(config)
        files.awsS3Bucket(signedUrlDuration = 1.days)
        cache.awsDynamoDb()
        secretBasis.generated()
        telemetrySettings.otelGrafanaCloud(
            instanceId = "1548711",
            zone = "prod-us-west-0",
        )
        grafanaAlerts(
            grafanaCloudStackSlug = "lightningkite",
            alerts = GrafanaAlert.httpErrors(displayName, errorThreshold = 5) +
                    GrafanaAlert.httpLatency(displayName, p99ThresholdMs = 5000) +
                    GrafanaAlert.httpLiveness(displayName),
        )
        cors.direct(
            CorsSettings(
                limitToDomains = listOf("*"),
                limitToHeaders = listOf("*"),
                limitToMethods = listOf("*"),
                allowCredentials = true,
                exposedHeaders = listOf(),
            )
        )
        notifications.byVariable()
        webUrl.direct("https://app.lskiteuistarter.cs.lightningkite.com")

        loggingSettings.direct(
            LoggingSettings(
                default = LoggingSettings.ContextSettings(
                    filePattern = null,
                    toConsole = true,
                    level = Level.DEBUG,
                    additive = false
                ),
                logger = mapOf(
                    "org.mongodb" to LoggingSettings.ContextSettings(
                        filePattern = null,
                        toConsole = true,
                        level = Level.DEBUG,
                        additive = false
                    ),
                    "software.amazon.awssdk" to LoggingSettings.ContextSettings(
                        filePattern = null,
                        toConsole = false,
                        level = Level.INFO,
                        additive = false
                    ),
                    "io.netty" to LoggingSettings.ContextSettings(
                        filePattern = null,
                        toConsole = false,
                        level = Level.INFO,
                        additive = false
                    ),
                )
            )
        )
    }
}

object LkEnvDeploy {
    @JvmStatic
    fun main(vararg args: String) {
        ProcessBuilder("./gradlew", "server:lambda").inheritIO().start().waitFor()
        LkEnv.deploy()
    }
}

object LkEnvEdit {
    @JvmStatic
    fun main(vararg args: String) = LkEnv.editVars()
}

object LkEnvPrepare {
    @JvmStatic
    fun main(vararg args: String): Unit = LkEnv.prepareTerraform().let(::println)
}