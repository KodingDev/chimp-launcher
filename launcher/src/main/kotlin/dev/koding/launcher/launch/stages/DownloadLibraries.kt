package dev.koding.launcher.launch.stages

import dev.koding.launcher.data.launcher.download
import dev.koding.launcher.data.minecraft.manifest.asDownload
import dev.koding.launcher.data.minecraft.manifest.assets
import dev.koding.launcher.data.minecraft.manifest.filterMatchesRule
import dev.koding.launcher.data.minecraft.manifest.native
import dev.koding.launcher.launch.*
import mu.KotlinLogging
import java.io.File

object DownloadLibraries : LaunchStage<DownloadLibraries.Result> {
    private val logger = KotlinLogging.logger {}

    override suspend fun run(launcher: MinecraftLauncher): Result {
        launcher.progressHandler("Downloading client jar", 0.0)
        logger.info { "Downloading client jar" }

        val libraryFolder = launcher.config[LibraryDirectory]
            ?: launcher.config[LauncherDirectory]?.resolve("libraries")
            ?: error("Launcher directory not specified")

        val clientJar =
            launcher.manifest.downloads?.client?.asDownload()?.download(
                libraryFolder.resolve("net/minecraft/version/${launcher.manifest.id}/version-${launcher.manifest.id}.jar"),
                strict = true
            ) ?: launcher.config[StartJarPath] ?: error("No client jar")

        logger.info { "Downloading libraries" }
        launcher.progressHandler("Downloading libraries", 0.0)
        launcher.manifest.libraries.filterMatchesRule()
            .flatMap { listOfNotNull(*it.assets.toTypedArray(), it.native) }
            .mapNotNull { it.asDownload() }
            .download(libraryFolder, progressHandler = launcher.progressHandler)

        return Result(clientJar, libraryFolder)
    }

    data class Result(
        val clientJar: File,
        val librariesFolder: File
    )
}