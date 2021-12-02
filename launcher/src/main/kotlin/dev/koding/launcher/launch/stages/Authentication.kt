package dev.koding.launcher.launch.stages

import dev.koding.launcher.auth.AuthData
import dev.koding.launcher.auth.AuthManager
import dev.koding.launcher.launch.AuthDirectory
import dev.koding.launcher.launch.LaunchStage
import dev.koding.launcher.launch.LauncherDirectory
import dev.koding.launcher.launch.MinecraftLauncher
import mu.KotlinLogging

object Authentication : LaunchStage<AuthData> {

    private val logger = KotlinLogging.logger {}

    override suspend fun run(launcher: MinecraftLauncher): AuthData {
        launcher.progressHandler("Authenticating", 0.0)
        logger.info { "Authenticating" }

        val authDir = launcher.config[AuthDirectory]
            ?: launcher.config[LauncherDirectory]?.resolve("auth")
            ?: error("Launcher directory not specified")
        return AuthManager(authDir).login()
    }

}