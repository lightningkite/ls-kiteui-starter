#!/usr/bin/env kotlin


import java.io.File
import kotlin.io.walkTopDown
import kotlin.sequences.distinct
import kotlin.sequences.forEach
import kotlin.sequences.plus

fun String.replaceWords(old: String, new: String): String {
    return this.replace(Regex("(?<!\\w)(${Regex.escape(old)})(?!\\w)"), new)
}


val serverImports = setOf(
    "kotlin.uuid.Uuid",
    "com.lightningkite.services.data.*",
    "com.lightningkite.services.database.*",
    "com.lightningkite.services.cache.*",
    "com.lightningkite.services.email.*",
    "com.lightningkite.services.sms.*",
    "com.lightningkite.services.files.*",
    "com.lightningkite.services.notifications.*",
    "com.lightningkite.lightningserver.*",
    "com.lightningkite.lightningserver.*",
    "com.lightningkite.lightningserver.deprecations.*",
    "com.lightningkite.lightningserver.definition.*",
    "com.lightningkite.lightningserver.definition.builder.*",
    "com.lightningkite.lightningserver.encryption.*",
    "com.lightningkite.lightningserver.http.*",
    "com.lightningkite.lightningserver.pathing.*",
    "com.lightningkite.lightningserver.runtime.*",
    "com.lightningkite.lightningserver.serialization.*",
    "com.lightningkite.lightningserver.settings.*",
    "com.lightningkite.lightningserver.websockets.*",
    "com.lightningkite.lightningserver.auth.*",
    "com.lightningkite.lightningserver.typed.*",
    "com.lightningkite.lightningserver.sessions.*",
)


val sharedImports = setOf(
    "kotlin.uuid.Uuid",
    "com.lightningkite.services.data.*",
    "com.lightningkite.services.database.*",
    "com.lightningkite.services.files.*",
    "com.lightningkite.lightningserver.*",
    "com.lightningkite.lightningserver.sessions.*",
)


val sharedBroken = listOf(
    "com.lightningkite.lightningdb",
    "com.lightningkite.lightningserver.files",
    "com.lightningkite.lightningserver.notifications"
)

val serverBroken = sharedBroken + listOf(
    "com.lightningkite.lightningserver"
)

val commonBroken = listOf(
    "com.lightningkite.UUID",
    "com.lightningkite.prepareModels",
    "com.lightningkite.now",
    "com.lightningkite.serialization"
)

fun String.broken(imports: List<String>) = imports.any { this.startsWith(it, ignoreCase = true) }

fun String.sharedBroken(): Boolean = broken(commonBroken) || broken(sharedBroken)
fun String.serverBroken(): Boolean = broken(commonBroken) || broken(serverBroken)

fun File.migrateServer() = walkTopDown()
    .filter { it.name.endsWith(".kt") }
    .forEach { file ->
        val text = file.readText()
        val imports = text.lineSequence()
            .filter { it.startsWith("import ") }
            .map { it.removePrefix("import ") }

        if (imports.none { it.serverBroken() || it.startsWith("com.lightningkite.", ignoreCase = true) }) return@forEach

        val fixedImports = imports
            .filter { !it.serverBroken() }
            .plus(serverImports)
            .distinct()
            .sorted()

        val preImports = text.substringBefore("import ")
        val postImports = text.substringAfterLast("import ").substringAfter('\n')
        val importCorrectedText = preImports + fixedImports.joinToString("\n") { "import $it" } + "\n" + postImports

        val repl = importCorrectedText
            .replaceWords("com.lightningkite.UUID", "kotlin.uuid.Uuid")
            .replaceWords("UUID", "Uuid")
            .replaceWords("AuthAccessor", "AuthAccess")
            .replaceWords("DatabaseSettings", "Database.Settings")
            .replaceWords("CacheSettings", "Cache.Settings")
            .replaceWords("FilesSettings", "PublicFileSystem.Settings")
            .replaceWords("SmsSettings", "SMS.Settings")
            .replaceWords("NotificationSettings", "NotificationService.Settings")
            .replaceWords("EmailSettings", "EmailService.Settings")
            .replaceWords("RequestAuth", "Authentication")
            .replaceWords("ServerPath", "PathSpec")
            .replace(".api(", " bind ApiHttpHandler(")
            .replace(".handler", " bind HttpHandler")
            .replace("authOptions = ", "auth = ")
            .replace("FieldCollection", "Table")
            .replace(".collection()", ".table()")
            .replace(".baseCollection()", ".baseTable()")
            .replace(".collectionName", ".tableName")
            .replace("EmailLabeledValue", "EmailAddressWithName")
            .replace(" Serialization", " serverRuntime.externalSerialization", ignoreCase = false)
            .replace(Regex("(?<!\\.)path\\((?<arg>.*)\\)")) { match ->
                "path.path(${match.groups["arg"]?.value ?: ""})"
            }
            .replace(".websocket(", " bind ")
            .replace(Regex("ModelRestEndpoints\\((?<path>\\w+),\\s*(?<info>\\w+)\\)")) { match ->
                "${match.groups["path"]?.value ?: ""} include ModelRestEndpoints(${match.groups["info"]?.value ?: ""})"
            }
            .replace(Regex("ModelRestUpdatesWebsocket\\((?<path>\\w+),\\s*(?<info>\\w+)\\)")) { match ->
                "${match.groups["path"]?.value ?: ""} include ModelRestUpdatesWebsocket(${match.groups["info"]?.value ?: ""})"
            }
            .replace(Regex("ServerPathGroup\\((.+)\\)")) { "ServerBuilder()" }
            .replace(Regex("\\s*class\\s*(?<name>\\w+)\\((?<path>\\w+)\\s*:\\s*(PathSpec|ServerPath)(,\\s*)?(?<otherArgs>.*)\\)\\s*:\\s*ServerBuilder\\(\\)")) { match ->
                val name = match.groups["name"]?.value ?: ""
                val otherArgs = match.groups["otherArgs"]?.value ?: ""

                if (otherArgs.isEmpty()) "\n\nobject $name : ServerBuilder()"
                else "\n\nclass $name($otherArgs) : ServerBuilder()"
            }
            .replace(Regex("[^\\S\\n]*(?<name>\\w+)\\((?<path>path\\.path\\(.*\\))(,\\s*)?(?<otherArgs>.*)\\)")) { match ->
                val name = match.groups["name"]?.value ?: ""
                val path = match.groups["path"]?.value ?: ""
                val otherArgs = match.groups["otherArgs"]?.value ?: ""

                if (otherArgs.isEmpty()) " $path include $name"
                else " $path include $name($path, $otherArgs)"
            }
            .replace(Regex("(?<space>\\s*)suspend\\s*fun\\s*(?<name>[\\w_]+)\\((?<args>.*)\\)")) { match ->
                val space = match.groups["space"]?.value ?: ""
                val name = match.groups["name"]?.value ?: ""
                val args = match.groups["args"]?.value ?: ""

                """
                    ||${space}context(runtime: ServerRuntime)
                    ||${space.substringAfterLast('\n')}suspend fun $name($args)
                """.trimMargin("||")
            }

        println("Fixed $file")
        file.writeText(repl)
    }

fun File.migrateShared() = walkTopDown()
    .filter { it.name.endsWith(".kt") }
    .forEach { file ->
        val text = file.readText()
        val imports = text.lineSequence()
            .filter { it.startsWith("import ") }
            .map { it.removePrefix("import ") }

        if (imports.none { it.sharedBroken() || it.startsWith("com.lightningkite.", ignoreCase = true) }) return@forEach

        val fixedImports = imports
            .filter { !it.sharedBroken() }
            .plus(sharedImports)
            .distinct()
            .sorted()

        val preImports = text.substringBefore("import ")
        val postImports = text.substringAfterLast("import ").substringAfter('\n')
        val importCorrectedText = preImports + fixedImports.joinToString("\n") { "import $it" } + "\n" + postImports

        val repl = importCorrectedText
            .replaceWords("com.lightningkite.UUID", "kotlin.uuid.Uuid")
            .replaceWords("UUID", "Uuid")

        println("Fixed $file")
        file.writeText(repl)
    }

//File("./server-ktor").migrateServer()
//File("./server-netty").migrateServer()
File("./server").migrateServer()
File("./apps").migrateShared()
File("./shared").migrateShared()