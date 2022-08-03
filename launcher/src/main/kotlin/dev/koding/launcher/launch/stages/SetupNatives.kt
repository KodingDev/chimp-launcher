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

package dev.koding.launcher.launch.stages

import dev.koding.launcher.data.minecraft.manifest.asDownload
import dev.koding.launcher.data.minecraft.manifest.asset
import dev.koding.launcher.data.minecraft.manifest.filterMatchesRule
import dev.koding.launcher.data.minecraft.manifest.nativeAsset
import dev.koding.launcher.launch.LaunchStage
import dev.koding.launcher.launch.MinecraftLauncher
import dev.koding.launcher.util.extractZip
import mu.KotlinLogging
import java.io.File

object SetupNatives : LaunchStage<SetupNatives.Result> {
    private val logger = KotlinLogging.logger {}

    override suspend fun run(launcher: MinecraftLauncher): Result {
        val downloadLibraries = launcher.get<DownloadLibraries.Result>(DownloadLibraries)
            ?: error("Failed to download libraries")
        val root = downloadLibraries.librariesFolder.resolve("net/minecraft/natives/${launcher.manifest.id}")

        logger.info { "Setting up natives" }
        val natives = launcher.manifest.libraries.filterMatchesRule()
            .mapNotNull { it.nativeAsset?.asDownload() ?: if (it.native) it.asset.asDownload() else null }

        if (root.exists()) root.deleteRecursively()
        root.mkdirs()

        natives.forEach {
            logger.debug { "Extracting natives: ${it.path} -> ${root.absolutePath}" }
            downloadLibraries.librariesFolder.resolve(it.path).extractZip(root)
        }

        return Result(root)
    }

    data class Result(
        val nativesFolder: File
    )
}