@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher

import dev.koding.launcher.data.assets.AssetIndex
import dev.koding.launcher.data.assets.toAsset
import dev.koding.launcher.data.manifest.*
import dev.koding.launcher.util.replaceParams
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File
import java.util.*

object Launcher {

    fun launch(manifest: LauncherManifest, root: File) {
        // TODO: Logging
        val libraries = manifest.libraries.filter { it.rules.matches(RuleAction.ALLOW) == RuleAction.ALLOW }

        val librariesFolder = File(root, "libraries")
        val assetsFolder = File(root, "assets")

        val assetIndex = AssetIndex.load(manifest.assetIndex.download(File(assetsFolder, "indexes")))
        val clientJar =
            manifest.downloads.client.download(File(root, "versions/${manifest.id}/${manifest.id}.jar"), strict = true)

        libraries.forEach {
            it.downloads.artifact.download(librariesFolder)
            it.downloads.classifiers?.let { classifiers ->
                classifiers.linuxNatives?.download(librariesFolder)
                classifiers.macosNatives?.download(librariesFolder)
                classifiers.windowsNatives?.download(librariesFolder)
            }
        }

        assetIndex.objects.entries.forEach { it.toAsset().download(File(assetsFolder, "objects")) }

        // TODO: Download Java

        val classpath = listOf(
            *libraries.flatMap { it.downloads.assets }
                .map { File(librariesFolder, it.path ?: "").absolutePath }
                .toTypedArray(),
            clientJar.absolutePath
        ).joinToString(separator = ":")

        val commandLine = listOf(
            "/Library/Java/JavaVirtualMachines/jdk-17.0.1.jdk/Contents/Home/bin/java",
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

        val process = ProcessBuilder(commandLine)
            .directory(root)
            .inheritIO()
            .start()
        process.waitFor()
    }
}


fun main() {
    val manifest = LauncherManifest.loadResource("/profile.json")
//    println(manifest)
    Launcher.launch(manifest, File("."))
}