package dev.koding.launcher.data.minecraft.manifest

import dev.koding.launcher.util.system.OS
import dev.koding.launcher.util.system.sha1
import mu.KotlinLogging
import java.io.File
import java.net.URL
import java.nio.file.Files

val logger = KotlinLogging.logger {}

fun Asset.matches(file: File): Boolean {
    if (size != null && Files.size(file.toPath()) != size) return false
    if (sha1 != null && sha1 != file.sha1) return false
    return true
}

fun Asset.getLocation(root: File, strict: Boolean = false): File {
    val url = URL(this.url)
    return if (strict) root
    else root.resolve(path ?: url.file.split("/").last())
}

fun Asset.download(root: File, strict: Boolean = false): File {
    val url = URL(this.url)
    val destination = getLocation(root, strict)

    // Check if the file already exists
    if (destination.exists() && matches(destination)) {
        logger.debug { "File already exists, skipping: ${destination.absolutePath}" }
        return destination
    }

    // Download the file
    logger.info { "Downloading file: $url (${(size ?: 0L) / 1_000.0}KB)" }
    url.openStream().use { input ->
        destination.parentFile.mkdirs()
        destination.outputStream().use { output -> input.copyTo(output) }
    }

    // Verify integrity
    if (!matches(destination)) throw IllegalStateException("Integrity check failed for $url")
    logger.debug { "Downloaded file: ${destination.absolutePath}" }
    return destination
}

fun Rule.matches(): Boolean {
    if (os != null) {
        if (os.name != null && OS.type.names.none { it == os.name }) return false
        if (os.version != null && OS.version.matches(os.version.toRegex())) return false
        if (os.arch != null && os.arch != OS.arch) return false
    }
    if (features != null) return false // future
    return true
}

fun List<Rule>.matches(default: Rule.Action): Rule.Action {
    if (isEmpty()) return Rule.Action.ALLOW
    var action = default
    forEach { if (it.matches()) action = it.action }
    return action
}

fun List<LauncherManifest.Arguments.Argument>.filterMatchesRule(default: Rule.Action) =
    filter { it.rules.matches(default) == Rule.Action.ALLOW }

fun List<LauncherManifest.Arguments.Argument>.toFilteredArray() =
    filterMatchesRule(Rule.Action.DISALLOW).map { it.value }.toTypedArray()

fun getJavaPath(root: File) = when (OS.type) {
    OS.Type.WINDOWS -> root.resolve("bin/java.exe")
    OS.Type.MAC -> root.resolve("jre.bundle/Contents/Home/bin/java")
    OS.Type.LINUX -> root.resolve("bin/java")
    else -> throw IllegalStateException("Unsupported OS: ${OS.type}")
}

operator fun LauncherManifest.Arguments.plus(other: LauncherManifest.Arguments) =
    LauncherManifest.Arguments(
        game = game.plus(other.game),
        jvm = jvm.plus(other.jvm)
    )

fun List<LauncherManifest.Library>.filterMatchesRule() =
    filter { it.rules.matches(Rule.Action.ALLOW) == Rule.Action.ALLOW }


val LauncherManifest.Library.asset: Asset?
    get() = downloads?.artifact ?: url?.let { url ->
        val path = name.split(":").let { "${it[0].replace(".", "/")}/${it[1]}/${it[2]}/${it[1]}-${it[2]}.jar" }
        Asset("$url$path", path = path)
    }

val LauncherManifest.Library.assets
    get() = listOfNotNull(
        asset,
        *(downloads?.classifiers?.values?.toTypedArray() ?: emptyArray())
    )
val LauncherManifest.Library.native
    get() = OS.type.names.mapNotNull { natives[it] }.firstOrNull()?.let { downloads?.classifiers?.get(it) }