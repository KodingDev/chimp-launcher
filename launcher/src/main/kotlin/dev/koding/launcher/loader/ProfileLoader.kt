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

package dev.koding.launcher.loader

import dev.koding.launcher.data.launcher.ProfileConfig
import dev.koding.launcher.data.launcher.ProgressHandler
import dev.koding.launcher.data.minecraft.manifest.LauncherManifest
import dev.koding.launcher.data.minecraft.manifest.plus
import dev.koding.launcher.frame.LauncherFrame
import dev.koding.launcher.launch.Config
import dev.koding.launcher.launch.GameDirectory
import dev.koding.launcher.launch.MinecraftLauncher
import dev.koding.launcher.launch.stages.StartGame
import mu.KotlinLogging
import java.net.URI

class ProfileLoader(
    private val profile: ProfileConfig,
    private val resourceManager: ResourceManager = ResourceManager(),
    private var build: MinecraftLauncher.() -> Unit = {}
) {

    private val logger = KotlinLogging.logger {}

    var progressHandler: ProgressHandler = { _, _ -> }
    val config = Config()

    suspend fun load() {
        logger.info { "Loading profile: ${profile.name}" }
        logger.info { "Loading resources" }

        resourceManager.config += config
        progressHandler("Loading resources", 0.0)
        profile.resources.forEachIndexed { i, it ->
            progressHandler(null, i / profile.resources.size.toDouble())

            try {
                resourceManager.load(it)
            } catch (e: Exception) {
                logger.error(e) { "Failed to load resource: $it" }
                LauncherFrame.main?.update("Download failed ($it): ${e.message}")
                throw e
            }
        }

        progressHandler("Loading files", 0.0)
        logger.info { "Loading files" }

        profile.files.entries.forEachIndexed { index, (path, data) ->
            progressHandler(null, index / profile.files.size.toDouble())
            val file = config[GameDirectory]?.resolve(path) ?: error("Game directory not specified")

            when (data.action) {
                ProfileConfig.File.Action.COPY -> {
                    val resource = data.resource?.let { resourceManager.load(URI(it)) }
                        ?: return@forEachIndexed
                    logger.debug { "Copying file: ${resource.file.absolutePath} -> ${file.absolutePath}" }
                    resource.file.copyTo(file, overwrite = true)
                }

                ProfileConfig.File.Action.DELETE -> {
                    if (!file.exists()) return@forEachIndexed
                    logger.debug { "Deleting file: ${file.absolutePath}" }
                    file.deleteRecursively()
                }
            }
        }
    }

    suspend fun start(): Process? {
        logger.info { "Starting profile: ${profile.name}" }
        val manifest = LauncherManifest.load(resourceManager, profile.launch.profile)
            .let {
                it.copy(
                    arguments = (it.arguments ?: LauncherManifest.Arguments()) + profile.launch.arguments,
                    minecraftArguments = it.minecraftArguments + profile.launch.arguments.game.joinToString(separator = " ")
                )
            }

        val launcher = MinecraftLauncher(manifest)
        launcher.config += config
        launcher.progressHandler = progressHandler
        launcher.build()
        return launcher.get<Process>(StartGame)
    }

    fun launcher(build: MinecraftLauncher.() -> Unit) {
        this.build = build
    }

}

fun ProfileConfig.loader(resourceManager: ResourceManager = ResourceManager(), build: ProfileLoader.() -> Unit = {}) =
    ProfileLoader(this, resourceManager).apply(build)