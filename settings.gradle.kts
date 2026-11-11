rootProject.name = "lskiteuistarter"

pluginManagement {
    repositories {
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
    }
    versionCatalogs {
        create("lk") {
            from("com.lightningkite:stack-catalog:1000.0.0")
        }
    }
}

include(":apps")
include(":apps-android")
include(":server")
include(":shared")
include(":integration-tests")
