@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher

import dev.koding.launcher.data.assets.AssetIndex
import dev.koding.launcher.data.assets.toAsset
import dev.koding.launcher.data.jdk.JdkFile
import dev.koding.launcher.data.jdk.JdkManifest
import dev.koding.launcher.data.manifest.*
import dev.koding.launcher.data.runtime.JavaRuntime
import dev.koding.launcher.data.runtime.match
import dev.koding.launcher.data.runtime.select
import dev.koding.launcher.util.replaceParams
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File
import java.nio.file.Files
import java.util.*

object Launcher {

    suspend fun launch(manifest: LauncherManifest, root: File) {
        val libraries = manifest.libraries.filter { it.rules.matches(RuleAction.ALLOW) == RuleAction.ALLOW }

        val librariesFolder = root.resolve("libraries")
        val assetsFolder = root.resolve("assets")

        val assetIndex = AssetIndex.load(manifest.assetIndex.download(assetsFolder.resolve("indexes")))
        val clientJar =
            manifest.downloads.client.download(
                root.resolve("versions/${manifest.id}/${manifest.id}.jar"),
                strict = true
            )

        libraries.forEach {
            it.downloads.artifact.download(librariesFolder)
            it.downloads.classifiers?.let { classifiers ->
                classifiers.linuxNatives?.download(librariesFolder)
                classifiers.macosNatives?.download(librariesFolder)
                classifiers.windowsNatives?.download(librariesFolder)
            }
        }

        assetIndex.objects.entries.forEach { it.toAsset().download(assetsFolder.resolve("objects")) }

        val javaHome = downloadJava(manifest, root) ?: error("Failed to download Java")

        val classpath = listOf(
            *libraries.flatMap { it.downloads.assets }
                .map { librariesFolder.resolve(it.path ?: "").absolutePath }
                .toTypedArray(),
            clientJar.absolutePath
        ).joinToString(separator = ":")

        val commandLine = listOf(
            manifest.javaVersion.getJavaPath(javaHome).absolutePath,
            *manifest.arguments.jvm.toFilteredArray(),
            manifest.mainClass,
            *manifest.arguments.game.toFilteredArray()
        ).map {
            it.replaceParams(
                "natives_directory" to ".",
                "launcher_name" to "chimp-launcher",
                "launcher_version" to "1.0.0",
                "classpath" to classpath,

                "auth_player_name" to "Test",
                "version_name" to manifest.id,
                "game_directory" to root.absolutePath, // TODO: Change this
                "assets_root" to assetsFolder.absolutePath,
                "assets_index_name" to manifest.assets,
                "auth_uuid" to UUID.randomUUID(),
                "auth_access_token" to "nonono",
                "user_type" to "mojang", // literally useless
                "version_type" to manifest.type
            )
        }

        // TODO: Fix warnings
        println(commandLine)
        val process = ProcessBuilder(commandLine)
            .directory(root)
            .inheritIO()
            .start()
        process.waitFor()
    }

    private suspend fun downloadJava(manifest: LauncherManifest, root: File): File? {
        val home = root.resolve("java/${manifest.javaVersion.component}/${manifest.javaVersion.majorVersion}")

        val runtime = JavaRuntime.load().select()
        val runtimeData = runtime?.get(manifest.javaVersion.component)?.match(manifest.javaVersion) ?: return null

        val jdkManifest = JdkManifest.load(runtimeData.manifest.download(home))
        jdkManifest.files.forEach { (path, data) ->
            when (data.type) {
                JdkFile.Type.DIRECTORY -> {
                    val dir = home.resolve(path)
                    if (!dir.exists()) dir.mkdirs()
                }
                JdkFile.Type.FILE -> {
                    val file = data.downloads?.raw?.download(home.resolve(path), strict = true) ?: return@forEach
                    if (data.executable == true) file.setExecutable(true)
                }
                JdkFile.Type.LINK -> {
                    val source = home.resolve(path).toPath()
                    val target = home.resolve(data.target ?: return@forEach).toPath()

                    if (Files.isSymbolicLink(source) || Files.isSymbolicLink(target)) return@forEach
                    Files.createSymbolicLink(source, target)
                }
            }
        }

        return home
    }
}


suspend fun main() {
    val manifest = LauncherManifest.loadResource("/profile.json")
    Launcher.launch(manifest, File("."))
}