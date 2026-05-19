// by Claude — JVM-only module for end-to-end integration tests:
// real Lightning Server (RAM database) + KiteUI frontend (via TestRunnerFetcher)
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
}

group = "com.lightningkite.lskiteuistarter"
version = "1.0-SNAPSHOT"

dependencies {
    // Server (gives us Server, TestRunner, endpoint definitions)
    // Server uses `implementation` for its deps, so we need them directly too
    implementation(project(":server"))

    // Lightning Server libraries (needed directly because :server uses implementation scope)
    implementation(libs.lightningServer.core)
    implementation(libs.lightningServer.typed)
    implementation(libs.lightningServer.sessions)
    implementation(libs.lightningServer.client.serverUtils)
    implementation(libs.services.database)

    // Frontend app code (gives us Pages, App, SDK, apiOverride)
    // Pull in the jvmSsr compilation from the multiplatform apps module
    implementation(project(":apps")) {
        targetConfiguration = "jvmSsrRuntimeElements"
    }

    // KiteUI library + test utilities (uiTest, UiTestScope, UiSnapshot, etc.)
    implementation(libs.kiteui)
    implementation(libs.kiteui.test)
    implementation(libs.coroutines.test)

    testImplementation(kotlin("test"))
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
