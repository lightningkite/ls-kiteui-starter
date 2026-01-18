package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.cors.CorsSettings
import com.lightningkite.lightningserver.definition.*
import com.lightningkite.lightningserver.engine.awsserverless.AwsAdapter
import com.lightningkite.lightningserver.terraform.*
import com.lightningkite.lightningserver.terraform.awsserverless.TerraformAwsServerlessDomainBuilder
import com.lightningkite.services.LoggingSettings
import com.lightningkite.services.cache.dynamodb.awsDynamoDb
import com.lightningkite.services.database.mongodb.mongodbAtlasFree
import com.lightningkite.services.email.javasmtp.awsSesSmtpLegacy
import com.lightningkite.services.files.s3.awsS3Bucket
import com.lightningkite.services.otel.OpenTelemetrySettings
import com.lightningkite.services.terraform.*
import com.lightningkite.toEmailAddress
import kotlinx.serialization.json.JsonObject
import software.amazon.awssdk.regions.Region
import java.io.File
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes


object LkEnv : TerraformAwsServerlessDomainBuilder<Server>(Server) {
    override val displayName = "LS KiteUI Starter"
    override val domain = "<your.domain.here>"
    override val domainZone = "<domain.zone.here>"
    override val terraformRoot: File = File("server/terraform/lk")

    override val handler: KClass<out AwsAdapter> = AwsHandler::class
    override val timeout: Duration = 5.minutes

    override val storageBucket = "<Storage Bucket Here>"
    override val storageBucketPath: String
        get() = super.storageBucketPath
    override val debug = true
    override val emergencyContact = "your@email.here".toEmailAddress()

    override val region = Region.US_WEST_2!!

    override val secretsSource: SecretSource = AwsSecretSource(projectPrefix, region)

    override fun Server.settings() {
        require(TerraformProviderImport.mongodbAtlas)
        require(TerraformProvider(TerraformProviderImport.mongodbAtlas, null, JsonObject(emptyMap())))
        println(this@LkEnv.terraformProviderImports)
        println(this@LkEnv.terraformProviders)

        loggingSettings.direct(LoggingSettings())
        database.mongodbAtlasFree(orgId = "<Org Id Here>")
        email.awsSesSmtpLegacy(emergencyContact)
        files.awsS3Bucket(signedUrlDuration = 1.days)
        cache.awsDynamoDb()
        secretBasis.generated()
        telemetrySettings.direct(OpenTelemetrySettings("console", batching = null))
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
        webUrl.direct("https://<your.frontend.url.here>")
    }
}

object DemoEnvDeploy {
    @JvmStatic
    fun main(vararg args: String) = LkEnv.deploy()
}

object DemoEnvEdit {
    @JvmStatic
    fun main(vararg args: String) = LkEnv.editVars()
}

object DemoEnvPrepare {
    @JvmStatic
    fun main(vararg args: String): Unit = LkEnv.prepareTerraform().let(::println)
}