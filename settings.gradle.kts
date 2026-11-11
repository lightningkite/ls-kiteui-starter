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
            from("com.lightningkite:stack-catalog:unreleased")
        }
    }
}

include(":apps")
include(":androidApp")
include(":server")
include(":shared")
include(":integration-tests")
