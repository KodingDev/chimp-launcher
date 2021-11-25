package dev.koding.launcher.loader

import dev.koding.launcher.LauncherFrame
import dev.koding.launcher.data.launcher.ProfileConfig
import dev.koding.launcher.data.minecraft.manifest.LauncherManifest
import dev.koding.launcher.data.minecraft.manifest.plus
import dev.koding.launcher.launch.Config
import dev.koding.launcher.launch.GameDirectory
import dev.koding.launcher.launch.MinecraftLauncher
import dev.koding.launcher.launch.stages.StartGame
import mu.KotlinLogging

class ProfileLoader(
    private val profile: ProfileConfig,
    private val resourceManager: ResourceManager = ResourceManager(),
    private val build: MinecraftLauncher.() -> Unit = {}
) {

    private val logger = KotlinLogging.logger {}

    val config = Config()

    suspend fun load() {
        resourceManager.config += config

        logger.info { "Loading profile: ${profile.name}" }
        logger.info { "Loading resources" }

        LauncherFrame.update("Loading resources")
        profile.resources.forEachIndexed { i, it ->
            LauncherFrame.updateProgress(i, profile.resources.size)
            resourceManager.load(it)
        }

        LauncherFrame.update("Loading files")
        logger.info { "Loading files" }

        profile.files.forEach { (path, data) ->
            LauncherFrame.updateProgress(profile.files.size, profile.files.size)
            val file = config[GameDirectory]?.resolve(path) ?: error("Game directory not specified")

            when (data.action) {
                ProfileConfig.File.Action.COPY -> {
                    val resource = data.resource?.let { resourceManager[it] } ?: return@forEach
                    logger.debug { "Copying file: ${resource.file.absolutePath} -> ${file.absolutePath}" }
                    resource.file.copyTo(file, overwrite = true)
                }
                ProfileConfig.File.Action.DELETE -> {
                    if (!file.exists()) return@forEach
                    logger.debug { "Deleting file: ${file.absolutePath}" }
                    file.delete()
                }
            }
        }
    }

    suspend fun start() {
        logger.info { "Starting profile: ${profile.name}" }
        val manifest = LauncherManifest.load(resourceManager, profile.launch.profile)
            .let {
                it.copy(
                    arguments = (it.arguments ?: LauncherManifest.Arguments()) + LauncherManifest.Arguments(game =
                    profile.launch.arguments.map { arg -> LauncherManifest.Arguments.Argument(arg) }),
                    minecraftArguments = it.minecraftArguments + profile.launch.arguments.joinToString(separator = " ")
                )
            }

        val launcher = MinecraftLauncher(manifest)
        launcher.config += config
        launcher.build()
        launcher.run(StartGame)
    }

}

fun ProfileConfig.loader(resourceManager: ResourceManager = ResourceManager(), build: ProfileLoader.() -> Unit = {}) =
    ProfileLoader(this, resourceManager).apply(build)