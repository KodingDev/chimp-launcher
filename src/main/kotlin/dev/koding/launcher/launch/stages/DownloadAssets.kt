@file:OptIn(ObsoleteCoroutinesApi::class)

package dev.koding.launcher.launch.stages

import dev.koding.launcher.LauncherFrame
import dev.koding.launcher.data.minecraft.assets.AssetIndex
import dev.koding.launcher.data.minecraft.assets.toAsset
import dev.koding.launcher.data.minecraft.manifest.download
import dev.koding.launcher.launch.*
import dev.koding.launcher.util.json
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.File

object DownloadAssets : LaunchStage<DownloadAssets.Result> {

    private val logger = KotlinLogging.logger {}

    override suspend fun run(launcher: MinecraftLauncher): Result {
        val folder = launcher.config[AssetsDirectory]
            ?: launcher.config[LauncherDirectory]?.resolve("assets")
            ?: error("Launcher directory not specified")

        LauncherFrame.update("Downloading asset index", 0)
        logger.info { "Downloading asset index" }
        val assetIndex = launcher.manifest.assetIndex?.download(folder.resolve("indexes"))?.json<AssetIndex>()
            ?: error("No asset index")

        LauncherFrame.update("Downloading assets", 0)
        logger.info { "Downloading assets" }

        var i = 0
        val context = newFixedThreadPoolContext(5, "Assets")
        assetIndex.objects.entries.map {
            CoroutineScope(context).async {
                it.toAsset().download(folder.resolve("objects"))
                LauncherFrame.updateProgress(i++, assetIndex.objects.size)
            }
        }.joinAll()

        return Result(folder)
    }

    data class Result(
        val folder: File
    ) : LaunchResult

}