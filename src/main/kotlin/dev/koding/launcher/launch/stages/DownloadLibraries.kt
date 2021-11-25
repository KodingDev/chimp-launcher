package dev.koding.launcher.launch.stages

import dev.koding.launcher.data.minecraft.manifest.assets
import dev.koding.launcher.data.minecraft.manifest.download
import dev.koding.launcher.data.minecraft.manifest.filterMatchesRule
import dev.koding.launcher.launch.LaunchStage
import dev.koding.launcher.launch.LauncherDirectory
import dev.koding.launcher.launch.LibraryDirectory
import dev.koding.launcher.launch.MinecraftLauncher
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
            launcher.manifest.downloads?.client?.download(
                libraryFolder.resolve("net/minecraft/version/${launcher.manifest.id}/version-${launcher.manifest.id}.jar"),
                strict = true
            ) ?: error("No client jar")

        logger.info { "Downloading libraries" }
        launcher.progressHandler("Downloading libraries", 0.0)
        launcher.manifest.libraries.filterMatchesRule().flatMap { it.assets }
            .download(libraryFolder, progressHandler = launcher.progressHandler)

        return Result(clientJar, libraryFolder)
    }

    data class Result(
        val clientJar: File,
        val librariesFolder: File
    )
}