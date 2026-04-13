#!/usr/bin/env kotlin

/**
 * Personalization Script for Lightning Kite Starter Project
 *
 * This script automates the process of personalizing the starter project by:
 * 1. Renaming all package declarations from com.lightningkite.lskiteuistarter to your package
 * 2. Updating all imports to reference the new package
 * 3. Updating Gradle build files with the new package and app name
 * 4. Updating AndroidManifest.xml with the new package
 * 5. Updating Firebase configuration files
 * 6. Moving source directory structures to match the new package
 *
 * Usage:
 *   1. Edit the configuration at the bottom of this file (uncomment the example)
 *   2. Run: kotlinc -script personalize.main.kts
 *
 * Alternative (from Gradle):
 *   Add a task to build.gradle.kts and run: ./gradlew personalize
 *
 * IMPORTANT: This script modifies many files. Make sure you have committed any
 * pending changes or created a backup before running!
 */

import java.io.File
import java.util.Locale

private val casingSeparatorRegex: Regex = Regex("([-_\\s]+([A-Z]*[a-z0-9]+))|([-_\\s]*[A-Z]+)")

private inline fun String.caseAlter(crossinline update: (after: String) -> String): String =
    casingSeparatorRegex.replace(this) {
        if (it.range.first == 0) it.value
        else update(it.value.filter { !(it == '-' || it == '_' || it.isWhitespace()) })
    }

private fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
private fun String.decapitalize(): String = replaceFirstChar { if (it.isUpperCase()) it.lowercase(Locale.getDefault()) else it.toString() }

fun String.camelCase(): String = caseAlter { it.capitalize() }.decapitalize()

val oldPackage = "com.lightningkite.lskiteuistarter"
val oldAppName = "LS KiteUI Starter"


data class Config(
    val appName: String,
    val packageName: String = "com.${appName.camelCase()}",
    val rootUsers: Set<String>,
    val appStoreTesterEmail: String = "appstoretester@${packageName.split('.').take(2).reversed().joinToString(".")}",
) {
    val extension = packageName.substringBefore('.')
    val domain = packageName.substringAfter('.')
    val appPackageName = packageName.substringAfterLast('.')
}

fun updateKotlinFiles(config: Config) {
    var fileCount = 0

    val packageRegex = Regex(oldPackage.split('.').joinToString("(.)"))
    val simpleDeclaration = oldPackage.substringAfterLast('.')
    val newSimpleDeclaration = config.appPackageName

    File(".").walk()
        .filter { it.extension == "kt" && !it.path.contains("build") }
        .forEach { file ->
            val originalText = file.readText()
            var text = originalText

            // Update package declaration
            text = text.replace(packageRegex) { match ->
                val separator = match.groups[1]?.value ?: "."
                config.packageName.replace(".", separator)
            }

            // with packages replaced now do simple declarations
            text = text.replace(simpleDeclaration, newSimpleDeclaration)

            // change app names
            text = text.replace(oldAppName, config.appName)

            if (text != originalText) {
                file.writeText(text)
                fileCount++
                println("  ✓ Updated: ${file.relativeTo(File(".")).path}")
            }
        }

    println("  Updated $fileCount Kotlin files")
}

fun updateAndroidManifest(config: Config) {
    val manifestFile = File("apps/src/androidMain/AndroidManifest.xml")
    if (!manifestFile.exists()) {
        println("  ⚠ AndroidManifest.xml not found, skipping")
        return
    }

    val originalText = manifestFile.readText()
    var text = originalText

    // Update package attribute
    text = text.replace(
        Regex("package=\"$oldPackage\""),
        "package=\"${config.packageName}\""
    )

    // Update any references in the manifest
    text = text.replace(oldPackage, config.packageName)

    if (text != originalText) {
        manifestFile.writeText(text)
        println("  ✓ Updated AndroidManifest.xml")
    } else {
        println("  - No changes needed")
    }
}

fun updateFirebaseConfig(config: Config) {
    val googleServicesFile = File("apps/google-services.json")
    if (!googleServicesFile.exists()) {
        println("  ⚠ google-services.json not found, skipping")
        return
    }

    val originalText = googleServicesFile.readText()
    var text = originalText

    // Update package name in google-services.json
    text = text.replace(
        Regex("\"package_name\"\\s*:\\s*\"$oldPackage\""),
        "\"package_name\": \"${config.packageName}\""
    )

    if (text != originalText) {
        googleServicesFile.writeText(text)
        println("  ✓ Updated google-services.json")
        println("  ⚠ WARNING: You should replace this with a new google-services.json from Firebase Console!")
    } else {
        println("  - No changes needed")
    }
}

fun movePackageDirectories(config: Config) {
    val oldParts = oldPackage.split(".")
    val newParts = config.packageName.split(".")

    val sourceRoots = listOf(
        "apps/src/commonMain/kotlin",
        "apps/src/androidMain/kotlin",
        "apps/src/iosMain/kotlin",
        "apps/src/jsMain/kotlin",
        "server/src/main/kotlin",
        "shared/src/commonMain/kotlin"
    )

    var dirCount = 0
    sourceRoots.forEach { sourceRoot ->
        val oldPath = File(sourceRoot, oldParts.joinToString("/"))
        if (!oldPath.exists() || !oldPath.isDirectory) return@forEach

        // Create new directory structure
        val newPath = File(sourceRoot, newParts.joinToString("/"))
        newPath.parentFile?.mkdirs()

        // Move the directory
        if (oldPath.renameTo(newPath)) {
            println("  ✓ Moved: ${oldPath.path} -> ${newPath.path}")
            dirCount++

            // Clean up old empty parent directories
            var parent = oldPath.parentFile
            while (parent != null && parent != File(sourceRoot)) {
                if (parent.listFiles()?.isEmpty() == true) {
                    parent.delete()
                    println("  ✓ Cleaned up empty directory: ${parent.path}")
                }
                parent = parent.parentFile
            }
        } else {
            // If rename failed, try copying
            println("  ⚠ Could not rename, attempting to copy...")
            if (copyRecursively(oldPath, newPath)) {
                (oldPath.parentFile ?: oldPath).deleteRecursively()
                println("  ✓ Copied and deleted: ${oldPath.path} -> ${newPath.path}")
                dirCount++
            } else {
                println("  ✗ Failed to move: ${oldPath.path}")
            }
        }
    }
    println("  Moved $dirCount package directories")
}


fun copyRecursively(source: File, target: File): Boolean {
    return try {
        if (source.isDirectory) {
            target.mkdirs()
            source.listFiles()?.forEach { child ->
                copyRecursively(child, File(target, child.name))
            }
            true
        } else {
            source.copyTo(target, overwrite = true)
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}



fun personalize(config: Config) {
    val newPackage = config.packageName

    println("Personalizing project:")
    println("  Old package: $oldPackage")
    println("  New package: $newPackage")
    println("  App name: ${config.appName}")
    println()

    // Step 1: Update all Kotlin files
    println("Step 1: Updating Kotlin files...")
    updateKotlinFiles(config)

    // Step 3: Update Android manifest
    println("\nStep 3: Updating AndroidManifest.xml...")
    updateAndroidManifest(config)

    // Step 4: Update Firebase configuration
    println("\nStep 4: Updating Firebase configuration...")
    updateFirebaseConfig(config)

    // Step 5: Move directory structures
    println("\nStep 5: Moving package directories...")
    movePackageDirectories(config)

    println("\n✓ Personalization complete!")
    println("\n" + "=".repeat(60))
    println("IMPORTANT: Manual steps required")
    println("=".repeat(60))
    println("\n1. Firebase Setup:")
    println("   - Create a new Firebase project at https://console.firebase.google.com")
    println("   - Add an Android app with package: $newPackage")
    println("   - Download new google-services.json and replace apps/google-services.json")
    println("   - Add an iOS app and download GoogleService-Info.plist for apps/src/iosMain/")
    println("\n2. Server Configuration:")
    println("   - Update settings.json with your database, email, and notification settings")
    println("   - Add Firebase service account JSON for server-side notifications")
    println("\n3. Regenerate SDK:")
    println("   - Run: ./gradlew :server:generateSdk")
    println("\n4. Update App Signing (optional):")
    println("   - Update local.properties with your signing configuration")
    println("\n5. Review Changes:")
    println("   - Check all modified files with: git status")
    println("   - Test the build: ./gradlew build")
    println("\n" + "=".repeat(60))
}

// Example usage:
// Uncomment and modify the configuration below, then run: kotlinc -script personalize.main.kts


val myConfig = Config(
    appName = "My Awesome App",
    packageName = "com.mycompany.myapp",
    rootUsers = setOf("admin@mycompany.com"),
    appStoreTesterEmail = "appstoretester@mycompany.com"
)

personalize(myConfig)
