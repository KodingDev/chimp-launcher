@file:OptIn(ObsoleteCoroutinesApi::class)

package dev.koding.launcher.launch.stages

import dev.koding.launcher.data.launcher.download
import dev.koding.launcher.data.minecraft.assets.AssetIndex
import dev.koding.launcher.data.minecraft.assets.toAsset
import dev.koding.launcher.data.minecraft.manifest.asDownload
import dev.koding.launcher.launch.AssetsDirectory
import dev.koding.launcher.launch.LaunchStage
import dev.koding.launcher.launch.LauncherDirectory
import dev.koding.launcher.launch.MinecraftLauncher
import dev.koding.launcher.util.json
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mu.KotlinLogging
import java.io.File

object DownloadAssets : LaunchStage<DownloadAssets.Result> {

    private val logger = KotlinLogging.logger {}

    override suspend fun run(launcher: MinecraftLauncher): Result {
        val folder = launcher.config[AssetsDirectory]
            ?: launcher.config[LauncherDirectory]?.resolve("assets")
            ?: error("Launcher directory not specified")

        launcher.progressHandler("Downloading asset index", 0.0)
        logger.info { "Downloading asset index" }
        val assetIndex = launcher.manifest.assetIndex?.asDownload()?.download(folder.resolve("indexes"))
            ?.json<AssetIndex>() ?: error("No asset index")

        logger.info { "Downloading assets" }
        launcher.progressHandler("Downloading assets", 0.0)
        assetIndex.objects.mapNotNull { it.toAsset().asDownload() }
            .download(folder.resolve("objects"), progressHandler = launcher.progressHandler, threads = 10)
        return Result(folder)
    }

    data class Result(
        val folder: File
    )

}