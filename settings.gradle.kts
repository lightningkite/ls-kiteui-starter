rootProject.name = "ls-kiteui-starter"

pluginManagement {
    repositories {
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}


include(":apps")
include(":server")
include(":shared")
include(":integration-tests") // by Claude — end-to-end tests: real server + KiteUI frontend
include(":load-tests") // by Claude — load tests: performance baselines against a live server
