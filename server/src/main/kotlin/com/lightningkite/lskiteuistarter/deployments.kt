package com.lightningkite.lskiteuistarter

import com.lightningkite.lightningserver.cors.CorsSettings
import com.lightningkite.lightningserver.definition.loggingSettings
import com.lightningkite.lightningserver.definition.secretBasis
import com.lightningkite.lightningserver.definition.telemetrySettings
import com.lightningkite.lightningserver.engine.awsserverless.AwsAdapter
import com.lightningkite.lightningserver.terraform.AwsSecretSource
import com.lightningkite.lightningserver.terraform.SecretSource
import com.lightningkite.lightningserver.terraform.awsserverless.TerraformAwsServerlessDomainBuilder
import com.lightningkite.lightningserver.terraform.generated
import com.lightningkite.services.LoggingSettings
import com.lightningkite.services.cache.dynamodb.awsDynamoDb
import com.lightningkite.services.database.mongodb.mongodbAtlasFree
import com.lightningkite.services.email.javasmtp.awsSesSmtp
import com.lightningkite.services.files.s3.awsS3Bucket
import com.lightningkite.services.otel.OpenTelemetrySettings
import com.lightningkite.services.terraform.TerraformProvider
import com.lightningkite.services.terraform.TerraformProviderImport
import com.lightningkite.services.terraform.byVariable
import com.lightningkite.services.terraform.direct
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
    override val domain = "api.lskiteuistarter.cs.lightningkite.com"
    override val domainZone = "cs.lightningkite.com"
    override val terraformRoot: File = File("server/terraform/lk")

    override val handler: KClass<out AwsAdapter> = AwsHandler::class
    override val timeout: Duration = 5.minutes

    override val storageBucket = "lightningkite-terraform"
    override val storageBucketPath: String
        get() = super.storageBucketPath
    override val debug = true
    override val emergencyContact = "joseph@lightningkite.com".toEmailAddress()

    override val region = Region.US_WEST_2!!

    override val secretsSource: SecretSource = AwsSecretSource(projectPrefix, region)

    override fun Server.settings() {
        require(TerraformProviderImport.mongodbAtlas)
        require(TerraformProvider(TerraformProviderImport.mongodbAtlas, null, JsonObject(emptyMap())))
        println(this@LkEnv.terraformProviderImports)
        println(this@LkEnv.terraformProviders)

        loggingSettings.direct(LoggingSettings())
        database.mongodbAtlasFree(orgId = "6323a65c43d66b56a2ea5aea")
        email.awsSesSmtp(emergencyContact.raw)
        files.awsS3Bucket(signedUrlDuration = 1.days)
        cache.awsDynamoDb()
        secretBasis.generated()
        telemetrySettings.direct(OpenTelemetrySettings("console", batching = null))
        cors.direct(CorsSettings(
            limitToDomains = listOf("*"),
            limitToHeaders = listOf("*"),
            limitToMethods = listOf("*"),
            allowCredentials = true,
            exposedHeaders = listOf(),
        ))
        notifications.byVariable()
        webUrl.direct("https://app.lskiteuistarter.cs.lightningkite.com")
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