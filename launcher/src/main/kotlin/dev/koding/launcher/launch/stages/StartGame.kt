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
import dev.koding.launcher.auth.CLIENT_ID
import dev.koding.launcher.data.minecraft.manifest.*
import dev.koding.launcher.launch.*
import dev.koding.launcher.util.replaceParams
import dev.koding.launcher.util.system.JavaUtil
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.File

object StartGame : LaunchStage<Process> {
    private val logger = KotlinLogging.logger {}
    private val coroutineScope = CoroutineScope(Dispatchers.IO) + Job()

    override suspend fun run(launcher: MinecraftLauncher): Process {
        val gameDir = launcher.config[GameDirectory] ?: error("Game directory not specified")
        val authentication = launcher.get<AuthData>(Authentication)
            ?: error("Authentication result is null")
        val downloadAssets = launcher.get<DownloadAssets.Result>(DownloadAssets)
            ?: error("Download assets result is null")
        val downloadLibraries = launcher.get<DownloadLibraries.Result>(DownloadLibraries)
            ?: error("Download libraries result is null")
        val downloadJava = launcher.get<DownloadJava.Result>(DownloadJava)
            ?: error("Download java result is null")
        val setupNatives = launcher.get<SetupNatives.Result>(SetupNatives)
            ?: error("Setup natives result is null")

        // I really need to rewrite half of this
        logger.info { "Launching version: ${launcher.manifest.id}" }

        val classpath = listOf(
            *launcher.manifest.libraries.filterMatchesRule()
                .flatMap { it.assets.filter { asset -> !it.isNative(asset) } }
                .mapNotNull { it.asDownload() }
                .map { downloadLibraries.librariesFolder.resolve(it.path).absolutePath }
                .toTypedArray(),
            downloadLibraries.clientJar.absolutePath
        ).distinct().joinToString(separator = File.pathSeparator)

        val commandLine = listOf(
            JavaUtil.getJavaPath(downloadJava.javaHome).absolutePath ?: error("No Java version"),
            *((launcher.manifest.arguments?.jvm?.toFilteredArray()?.takeUnless { it.isEmpty() }
                ?: arrayOf("-Djava.library.path=\${natives_directory}", "-cp", "\${classpath}"))
                    + (launcher.config[ExtraArgs]?.jvm?.toFilteredArray() ?: emptyArray())),
            launcher.manifest.mainClass,
            *((launcher.manifest.arguments?.game?.toFilteredArray()?.takeUnless { it.isEmpty() }
                ?: launcher.manifest.minecraftArguments?.split(" ")?.toTypedArray()
                ?: emptyArray()) + (launcher.config[ExtraArgs]?.game?.toFilteredArray() ?: emptyArray())),
        ).map {
            it.replaceParams(
                "natives_directory" to setupNatives.nativesFolder.absolutePath,
                "library_directory" to downloadLibraries.librariesFolder,
                "launcher_name" to "chimp-launcher",
                "launcher_version" to "1.0.0",
                "classpath" to classpath,

                "auth_player_name" to authentication.profile.name,
                "version_name" to launcher.manifest.id,
                "game_directory" to gameDir,
                "assets_root" to downloadAssets.folder.absolutePath,
                "assets_index_name" to (launcher.manifest.assets ?: error("No assets index")),
                "auth_uuid" to authentication.profile.id,
                "auth_access_token" to authentication.token.accessToken,
                "user_type" to "mojang",
                "version_type" to launcher.manifest.type,
                "user_properties" to "{}",
                "clientid" to CLIENT_ID,
                "auth_xuid" to "", // May be needed in the future?
                "classpath_separator" to File.pathSeparator
            )
        }

        launcher.config[StartupHandler]?.let {
            logger.debug { "Running startup handler: $it" }
            it(launcher)
        }

        logger.info { "Launching Minecraft" }
        logger.debug { "Command line: ${commandLine.joinToString(separator = " ")}" }

        if (!gameDir.exists()) gameDir.mkdirs()
        val process = withContext(Dispatchers.IO) {
            ProcessBuilder(commandLine)
                .directory(gameDir)
                .start()
        }

        coroutineScope.launch {
            // Log the process output
            process.inputStream.bufferedReader().forEachLine {
                logger.info { it }
            }
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            if (!process.isAlive) return@Thread
            logger.info { "Killing Minecraft" }
            process.destroy()
        })
        return process
    }
}