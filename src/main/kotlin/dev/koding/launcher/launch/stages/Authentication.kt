package dev.koding.launcher.launch.stages

import dev.koding.launcher.LauncherFrame
import dev.koding.launcher.auth.AuthData
import dev.koding.launcher.auth.AuthManager
import dev.koding.launcher.launch.*
import mu.KotlinLogging

object Authentication : LaunchStage<Authentication.Result> {

    private val logger = KotlinLogging.logger {}

    override suspend fun run(launcher: MinecraftLauncher): Result {
        LauncherFrame.update("Authenticating", 0)
        logger.info { "Authenticating" }

        val authDir = launcher.config[AuthDirectory]
            ?: launcher.config[LauncherDirectory]?.resolve("auth")
            ?: error("Launcher directory not specified")
        return Result(AuthManager(authDir).login())
    }

    data class Result(
        val data: AuthData
    ) : LaunchResult

}