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
//            from("com.lightningkite:stack-catalog:1000.+") // Auto Minor Updates
//            from("com.lightningkite:stack-catalog:1001.0.+") // Auto Patch updates only
            from("com.lightningkite:stack-catalog:local") // Working on a local catalog
        }
    }
}

include(":apps")
include(":apps-android")
include(":server")
include(":shared")
include(":integration-tests")
