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
    implementation(lk.lightningserver.core.asProvider())
    implementation(lk.lightningserver.typed.asProvider())
    implementation(lk.lightningserver.sessions.asProvider())
    implementation(lk.lightningserver.server.client.utils)
    implementation(lk.services.database.asProvider())
    implementation(lk.services.email.asProvider())

    // Frontend app code (Pages, App, generated SDK, apiOverride) — the headless jvmSsr compilation.
    implementation(project(":apps")) {
        targetConfiguration = "jvmSsrRuntimeElements"
    }

    // KiteUI library + test utilities (uiTest, UiTestScope, UiSnapshot).
    implementation(lk.kiteui.library.asProvider())
    implementation(lk.kiteui.test.utilities)
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
