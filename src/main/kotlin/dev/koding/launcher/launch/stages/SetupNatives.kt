package dev.koding.launcher.launch.stages

import dev.koding.launcher.data.minecraft.manifest.filterMatchesRule
import dev.koding.launcher.data.minecraft.manifest.getLocation
import dev.koding.launcher.data.minecraft.manifest.native
import dev.koding.launcher.launch.LaunchResult
import dev.koding.launcher.launch.LaunchStage
import dev.koding.launcher.launch.MinecraftLauncher
import dev.koding.launcher.util.system.extractZip
import mu.KotlinLogging
import java.io.File

object SetupNatives : LaunchStage<SetupNatives.Result> {
    private val logger = KotlinLogging.logger {}

    override suspend fun run(launcher: MinecraftLauncher): Result {
        val downloadLibraries = launcher.get<DownloadLibraries.Result>(DownloadLibraries)
            ?: error("Failed to download libraries")
        val root = downloadLibraries.librariesFolder.resolve("net/minecraft/natives/${launcher.manifest.id}")

        logger.info { "Setting up natives" }
        val natives = launcher.manifest.libraries.filterMatchesRule().mapNotNull { it.native }

        if (root.exists()) root.deleteRecursively()
        root.mkdirs()

        natives.forEach {
            logger.debug { "Extracting natives: ${it.path} -> ${root.absolutePath}" }
            it.getLocation(downloadLibraries.librariesFolder).extractZip(root)
        }

        return Result(root)
    }

    data class Result(
        val nativesFolder: File
    ) : LaunchResult
}