// by Claude — Deployment configuration template. Update all placeholder values for your project.
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
import com.lightningkite.services.email.javasmtp.awsSesDomain
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


// TODO: Update all placeholder values below for your project
object LkEnv : TerraformAwsServerlessDomainBuilder<Server>(Server) {
    // TODO: Change to your project's display name
    override val displayName = "My App"

    // TODO: Change to your API domain
    override val domain = "api.myapp.example.com"

    // TODO: Change to your Route53 hosted zone
    override val domainZone = "example.com"

    override val terraformRoot: File = File("server/terraform/lk")

    override val handler: KClass<out AwsAdapter> = AwsHandler::class
    override val timeout: Duration = 5.minutes

    // TODO: Change to your S3 bucket for Terraform state
    override val storageBucket = "my-terraform-state-bucket"
    override val storageBucketPath: String
        get() = super.storageBucketPath

    // TODO: Set to false for production
    override val debug = true

    // TODO: Change to your emergency contact email
    override val emergencyContact = "you@example.com".toEmailAddress()

    // TODO: Change to your preferred AWS region
    override val region = Region.US_WEST_2!!

    // TODO: Update AWS profile name if needed
    override val secretsSource: SecretSource = AwsSecretSource("default", projectPrefix, region)

    override fun Server.settings() {
        require(TerraformProviderImport.mongodbAtlas)
        require(TerraformProvider(TerraformProviderImport.mongodbAtlas, null, JsonObject(emptyMap())))

        loggingSettings.direct(LoggingSettings())

        // TODO: Update MongoDB Atlas org ID
        database.mongodbAtlasFree(orgId = "YOUR_MONGODB_ATLAS_ORG_ID")

        awsSesDomain("email", emergencyContact)
        email.awsSesSmtp("email")
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

        // TODO: Change to your web app URL
        webUrl.direct("https://app.myapp.example.com")
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
