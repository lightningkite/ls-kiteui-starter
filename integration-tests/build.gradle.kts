// JVM-only module for end-to-end integration tests:
// real Lightning Server (RAM database) + KiteUI frontend driven in-process via TestRunnerFetcher.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.lightningkite.lskiteuistarter"
version = "1.0-SNAPSHOT"

dependencies {
    // Server: gives us Server, TestRunner, endpoint definitions.
    // :server uses `implementation` scope for its deps, so we depend on the Lightning Server
    // libraries directly too (TestRunner, ServerRuntime, Database.Settings, session creation).
    implementation(project(":server"))
    implementation(libs.lightningServer.core)
    implementation(libs.lightningServer.typed)
    implementation(libs.lightningServer.sessions)
    implementation(libs.lightningServer.client.serverUtils)
    implementation(libs.services.database)
    implementation(libs.services.email)

    // Frontend app code (Pages, App, generated SDK, apiOverride) — the headless jvmSsr compilation.
    implementation(project(":apps")) {
        targetConfiguration = "jvmSsrRuntimeElements"
    }

    // KiteUI library + test utilities (uiTest, UiTestScope, UiSnapshot).
    implementation(libs.kiteui)
    implementation(libs.kiteui.test)
    implementation(libs.kotlinx.coroutines.test)

    testImplementation(kotlin("test"))
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
