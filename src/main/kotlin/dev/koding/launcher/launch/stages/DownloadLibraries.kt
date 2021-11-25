package dev.koding.launcher.launch.stages

import dev.koding.launcher.LauncherFrame
import dev.koding.launcher.data.minecraft.manifest.assets
import dev.koding.launcher.data.minecraft.manifest.download
import dev.koding.launcher.data.minecraft.manifest.filterMatchesRule
import dev.koding.launcher.launch.*
import mu.KotlinLogging
import java.io.File

object DownloadLibraries : LaunchStage<DownloadLibraries.Result> {
    private val logger = KotlinLogging.logger {}

    override suspend fun run(launcher: MinecraftLauncher): Result {
        // TODO: Remove these
        LauncherFrame.update("Downloading client jar", 0)
        logger.info { "Downloading client jar" }

        val libraryFolder = launcher.config[LibraryDirectory]
            ?: launcher.config[LauncherDirectory]?.resolve("libraries")
            ?: error("Launcher directory not specified")

        val clientJar =
            launcher.manifest.downloads?.client?.download(
                libraryFolder.resolve("net/minecraft/version/${launcher.manifest.id}/version-${launcher.manifest.id}.jar"),
                strict = true
            ) ?: error("No client jar")

        LauncherFrame.update("Downloading libraries", 0)
        logger.info { "Downloading libraries" }

        val libraries = launcher.manifest.libraries.filterMatchesRule()
        libraries.forEachIndexed { index, it ->
            LauncherFrame.updateProgress(index, libraries.size)
            it.assets.forEach { it.download(libraryFolder) }
        }

        return Result(clientJar, libraryFolder)
    }

    data class Result(
        val clientJar: File,
        val librariesFolder: File
    ) : LaunchResult
}