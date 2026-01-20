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
