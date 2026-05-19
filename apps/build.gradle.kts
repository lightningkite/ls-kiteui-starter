import com.lightningkite.kiteui.KiteUiPluginExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.nio.file.Files
import java.util.*
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import kotlin.collections.set

// KMP currently doesn't disable iOS target and dependency resolution correctly when not on a mac.
// So we work around it on non mac machines with this check
val onMac = System.getProperty("os.name").contains("Mac", ignoreCase = true)

plugins {
    alias(libs.plugins.androidApp)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.serialization)
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        alias(libs.plugins.kotlinCocoapods)
    }
    alias(libs.plugins.kiteui)
    alias(libs.plugins.kjsplain)
    alias(libs.plugins.kfc)
}

group = "com.lightningkite.lskiteuistarter"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://jitpack.io")
}

kotlin {
    applyDefaultHierarchyTemplate()
    androidTarget()
    if (onMac) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }
    js {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kiteui)
                api(libs.csvDurable)
                api(libs.lightningServer.core.shared)
                api(libs.lightningServer.typed.shared)
                api(libs.lightningServer.sessions.shared)
                api(libs.lightningServer.client)
                api(project(":shared"))
            }
        }
        val androidMain by getting {
            dependencies {
                api(libs.firebase.messaging.ktx)
            }
        }
        if (onMac) {
            val iosMain by getting {
                dependencies {
                }
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(npm("firebase", "10.7.1"))
            }
        }


        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }

    if (onMac) {
        // We have to manually call this because the shortcut isn't available when plugin is not applied and gradle will
        // fail even behind the if check
        (this as ExtensionAware).extensions.configure<CocoapodsExtension>("cocoapods", {
            // Required properties
            // Specify the required Pod version here. Otherwise, the Gradle project version is used.
            version = "1.0"
            summary = "Some description for a Kotlin/Native module"
            homepage = "Link to a Kotlin/Native module homepage"
            ios.deploymentTarget = "14.0"

            // Optional properties
            // Configure the Pod name here instead of changing the Gradle project name
            name = "apps"

            framework {
                baseName = "apps"
                export(project(":shared"))
                export(libs.kiteui)
                export(libs.lightningServer.client)
//            podfile = project.file("../example-app-ios/Podfile")
            }
        })
    }

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

android {
    namespace = "com.lightningkite.lskiteuistarter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lightningkite.lskiteuistarter"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources.excludes.add("com/lightningkite/lightningserver/lightningdb.txt")
        resources.excludes.add("com/lightningkite/lightningserver/lightningdb-log.txt")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    val props = project.rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { stream ->
        Properties().apply { load(stream) }
    }
    if (props != null && props.getProperty("signingKeystore") != null) {
        signingConfigs {
            this.create("release") {
                storeFile = project.rootProject.file(props.getProperty("signingKeystore"))
                storePassword = props.getProperty("signingPassword")
                keyAlias = props.getProperty("signingAlias")
                keyPassword = props.getProperty("signingAliasPassword")
            }
        }
        buildTypes {
            this.getByName("release") {
                this.isMinifyEnabled = false
                this.proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
                this.signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    dependencies {
        coreLibraryDesugaring(libs.desugarJdkLibs)
    }
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<YarnRootExtension>().yarnLockMismatchReport =
        YarnLockMismatchReport.WARNING
    rootProject.the<YarnRootExtension>().reportNewYarnLock = true
    rootProject.the<YarnRootExtension>().yarnLockAutoReplace = true
}

configure<KiteUiPluginExtension> {
    this.packageName = "com.lightningkite.lskiteuistarter"
    this.iosProjectRoot = project.file("./ios/app")
}

// Create symlink for Kotlin/JS source maps to resolve correctly
// Source maps reference paths like ../../../../../../src/jsMain/kotlin/... which resolve to build/js/packages/src/...
// This symlink redirects those requests to the actual source location
tasks.register("setupSourceMapSymlink") {
    val packagesDir = rootProject.file("build/js/packages")
    val symlinkPath = packagesDir.resolve("src")
    val targetPath = rootProject.file("apps/src")

    doLast {
        if (!packagesDir.exists()) {
            packagesDir.mkdirs()
        }
        if (symlinkPath.exists()) {
            if (Files.isSymbolicLink(symlinkPath.toPath())) {
                return@doLast // Already set up
            }
            symlinkPath.delete()
        }
        Files.createSymbolicLink(
            symlinkPath.toPath(),
            symlinkPath.parentFile.toPath().relativize(targetPath.toPath())
        )
        println("Created source map symlink: $symlinkPath -> $targetPath")
    }
}

tasks.matching { it.name == "jsViteDev" || it.name == "jsBrowserDevelopmentRun" }.configureEach {
    dependsOn("setupSourceMapSymlink")
}

fun env(name: String, profile: String) {
    tasks.create("deployWeb${name}Init", Exec::class.java) {
        group = "deploy"
        this.dependsOn("viteBuild")
        this.environment("AWS_PROFILE", profile)
        val props = Properties()
        props.entries.forEach { environment(it.key.toString().trim('"', ' '), it.value.toString().trim('"', ' ')) }
        this.executable = "terraform"
        this.args("init")
        this.workingDir = file("terraform/$name")
    }
    tasks.create("deployWeb${name}", Exec::class.java) {
        group = "deploy"
        this.dependsOn("deployWeb${name}Init")
        this.environment("AWS_PROFILE", profile)
        val props = Properties()
        props.entries.forEach { environment(it.key.toString().trim('"', ' '), it.value.toString().trim('"', ' ')) }
        this.executable = "terraform"
        this.args("apply", "-auto-approve")
        this.workingDir = file("terraform/$name")
    }
}

env("default", "default")

// by Claude — Fastlane deploy tasks
//
// These tasks read credentials from local.properties and pass them as env vars
// to the Fastlane subprocess, keeping local.properties as the single config file.
//
// Required local.properties keys:
//   playStoreJsonKeyPath   – path to Google Play service account JSON key
//   ios.teamId             – 10-char Apple Developer Team ID
//   ios.ascKeyId           – App Store Connect API key ID
//   ios.ascIssuerId        – App Store Connect API issuer ID
//   ios.ascKeyPath         – path to the .p8 API key file (e.g. local/AuthKey_XYZ.p8)
//   ios.matchS3Bucket      – S3 bucket name for Fastlane match certificate storage
//   ios.matchS3Region      – S3 region (default: us-west-2)
//   ios.matchPassword      – AES-256 passphrase match uses to encrypt files in S3
//
// Usage:
//   ./gradlew :apps:publishAndroid          – build AAB + upload to Play internal track
//   ./gradlew :apps:promoteAndroid          – promote Play internal → production
//   ./gradlew :apps:setupMatch              – one-time: generate iOS cert + profile → S3
//   ./gradlew :apps:publishIos              – match certs + build + upload to TestFlight
//   ./gradlew :apps:submitIos               – submit latest TestFlight build for review
// ─────────────────────────────────────────────────────────────────────────────

val localProperties = project.rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { stream ->
    Properties().apply { load(stream) }
}

// Reads the .p8 key file and base64-encodes it for passing as an env var to Fastlane.
// Returns empty string if the path isn't configured or the file doesn't exist yet.
fun ascKeyContent(): String {
    val path = localProperties?.getProperty("ios.ascKeyPath") ?: return ""
    val file = rootProject.file(path)
    return if (file.exists()) Base64.getEncoder().encodeToString(file.readBytes()) else ""
}

tasks.register<Exec>("publishAndroid") {
    group = "deploy"
    description = "Build signed AAB then upload to Play Store internal track via Fastlane"
    dependsOn("bundleRelease")  // Gradle builds + signs; Fastlane only uploads
    workingDir = rootProject.projectDir
    environment("PLAY_STORE_JSON_KEY_PATH", localProperties?.getProperty("playStoreJsonKeyPath") ?: "")
    commandLine("bundle", "exec", "fastlane", "android", "internal")
}

tasks.register<Exec>("promoteAndroid") {
    group = "deploy"
    description = "Promote the current Play Store internal track build to production via Fastlane"
    workingDir = rootProject.projectDir
    environment("PLAY_STORE_JSON_KEY_PATH", localProperties?.getProperty("playStoreJsonKeyPath") ?: "")
    commandLine("bundle", "exec", "fastlane", "android", "promote_to_production")
}

// Shared AWS + match env vars for all iOS tasks - by Claude
fun ExecSpec.iosEnvironment() {
    val awsProfile = localProperties?.getProperty("ios.awsProfile") ?: ""
    val s3Region   = localProperties?.getProperty("ios.matchS3Region") ?: "us-west-2"
    if (awsProfile.isNotBlank()) environment("AWS_PROFILE", awsProfile)
    environment("AWS_DEFAULT_REGION", s3Region)
    environment("MATCH_S3_BUCKET",    localProperties?.getProperty("ios.matchS3Bucket") ?: "")
    environment("MATCH_S3_REGION",    s3Region)
    environment("MATCH_PASSWORD",     localProperties?.getProperty("ios.matchPassword") ?: "")
    environment("ASC_KEY_ID",         localProperties?.getProperty("ios.ascKeyId") ?: "")
    environment("ASC_ISSUER_ID",      localProperties?.getProperty("ios.ascIssuerId") ?: "")
    environment("ASC_KEY_CONTENT",    ascKeyContent())
}

tasks.register<Exec>("setupMatch") {
    group = "deploy"
    description = "One-time setup: generate iOS distribution cert + provisioning profile and store in S3"
    workingDir = rootProject.projectDir
    iosEnvironment()
    environment("IOS_TEAM_ID", localProperties?.getProperty("ios.teamId") ?: "")
    commandLine("bundle", "exec", "fastlane", "ios", "setup_match")
}

tasks.register<Exec>("publishIos") {
    group = "deploy"
    description = "Sync match certs, build, and upload iOS app to TestFlight via Fastlane"
    workingDir = rootProject.projectDir
    iosEnvironment()
    environment("IOS_TEAM_ID", localProperties?.getProperty("ios.teamId") ?: "")
    commandLine("bundle", "exec", "fastlane", "ios", "beta")
}

tasks.register<Exec>("submitIos") {
    group = "deploy"
    description = "Submit the latest TestFlight build for App Store review via Fastlane"
    workingDir = rootProject.projectDir
    iosEnvironment()
    commandLine("bundle", "exec", "fastlane", "ios", "submit")
}