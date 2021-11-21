package dev.koding.launcher.loader

import dev.koding.launcher.Launcher
import dev.koding.launcher.data.config.ProfileConfig
import dev.koding.launcher.data.config.ProfileFile
import dev.koding.launcher.data.manifest.LaunchArgument
import dev.koding.launcher.data.manifest.LaunchArguments
import dev.koding.launcher.data.manifest.plus
import mu.KotlinLogging

class ProfileLoader(private val config: ProfileConfig) {

    private val logger = KotlinLogging.logger {}
    private val resourceManager = ResourceManager()
    private val profileHome = Launcher.home.resolve("profiles/${config.name}")

    suspend fun load() {
        logger.info { "Loading profile: ${config.name}" }
        logger.info { "Loading resources" }
        config.resources.forEach { resourceManager.load(it) }

        logger.info { "Loading files" }
        config.files.forEach { (path, data) ->
            val file = profileHome.resolve(path)
            when (data.action) {
                ProfileFile.Action.COPY -> {
                    val resource = data.resource?.let { resourceManager[it] } ?: return@forEach
                    logger.debug { "Copying file: ${resource.file.absolutePath} -> ${file.absolutePath}" }
                    resource.file.copyTo(file, overwrite = true)
                }
                ProfileFile.Action.DELETE -> {
                    if (!file.exists()) return@forEach
                    logger.debug { "Deleting file: ${file.absolutePath}" }
                    file.delete()
                }
            }
        }
    }

    suspend fun start() {
        logger.info { "Starting profile: ${config.name}" }
        val manifest = resourceManager.getManifest(config.launch.profile)
        Launcher.launch(
            manifest.copy(
                arguments = manifest.arguments + LaunchArguments(game =
                config.launch.arguments.map { LaunchArgument(it) })
            ), profileHome
        )
    }

}