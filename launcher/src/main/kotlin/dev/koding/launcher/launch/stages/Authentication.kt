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