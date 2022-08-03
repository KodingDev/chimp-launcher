/*
 *    Copyright 2022 Koding Dev
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
        val assetIndex = launcher.manifest.assetIndex?.asDownload()
            ?.let {
                val target = folder.resolve("indexes/${it.path.substringAfterLast('/')}")
                it.download(target, strict = true)
            }
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