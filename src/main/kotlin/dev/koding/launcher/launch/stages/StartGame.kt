package dev.koding.launcher.launch.stages

import dev.koding.launcher.LauncherFrame
import dev.koding.launcher.data.minecraft.manifest.assets
import dev.koding.launcher.data.minecraft.manifest.filterMatchesRule
import dev.koding.launcher.data.minecraft.manifest.getJavaPath
import dev.koding.launcher.data.minecraft.manifest.toFilteredArray
import dev.koding.launcher.launch.GameDirectory
import dev.koding.launcher.launch.LaunchStage
import dev.koding.launcher.launch.LaunchSuccess
import dev.koding.launcher.launch.MinecraftLauncher
import dev.koding.launcher.util.replaceParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File

object StartGame : LaunchStage<LaunchSuccess> {
    private val logger = KotlinLogging.logger {}

    override suspend fun run(launcher: MinecraftLauncher): LaunchSuccess {
        val gameDir = launcher.config[GameDirectory] ?: error("Game directory not specified")
        val authentication = launcher.get<Authentication.Result>(Authentication)
            ?: error("Authentication result is null")
        val downloadAssets = launcher.get<DownloadAssets.Result>(DownloadAssets)
            ?: error("Download assets result is null")
        val downloadLibraries = launcher.get<DownloadLibraries.Result>(DownloadLibraries)
            ?: error("Download libraries result is null")
        val downloadJava = launcher.get<DownloadJava.Result>(DownloadJava)
            ?: error("Download java result is null")
        val setupNatives = launcher.get<SetupNatives.Result>(SetupNatives)
            ?: error("Setup natives result is null")

        // TODO: Rewrite half of this
        logger.info { "Launching version: ${launcher.manifest.id}" }

        val classpath = listOf(
            *launcher.manifest.libraries.filterMatchesRule()
                .flatMap { it.assets }
                .map { downloadLibraries.librariesFolder.resolve(it.path ?: "").absolutePath }
                .toTypedArray(),
            downloadLibraries.clientJar.absolutePath
        ).joinToString(separator = File.pathSeparator)

        val commandLine = listOf(
            getJavaPath(downloadJava.javaHome).absolutePath ?: error("No Java version"),
            *launcher.manifest.arguments?.jvm?.toFilteredArray()?.takeUnless { it.isEmpty() }
                ?: arrayOf("-Djava.library.path=\${natives_directory}", "-cp", "\${classpath}"),
            launcher.manifest.mainClass,
            *(launcher.manifest.arguments?.game?.toFilteredArray()?.takeUnless { it.isEmpty() }
                ?: launcher.manifest.minecraftArguments?.split(" ")?.toTypedArray()
                ?: emptyArray()),
        ).map {
            it.replaceParams(
                "natives_directory" to setupNatives.nativesFolder.absolutePath,
                "launcher_name" to "chimp-launcher",
                "launcher_version" to "1.0.0",
                "classpath" to classpath,

                "auth_player_name" to authentication.data.profile.name,
                "version_name" to launcher.manifest.id,
                "game_directory" to gameDir,
                "assets_root" to downloadAssets.folder.absolutePath,
                "assets_index_name" to (launcher.manifest.assets ?: error("No assets index")),
                "auth_uuid" to authentication.data.profile.id,
                "auth_access_token" to authentication.data.token.accessToken,
                "user_type" to "mojang",
                "version_type" to launcher.manifest.type,
                "user_properties" to "{}"
            )
        }

        logger.info { "Launching Minecraft" }
        logger.debug { "Command line: ${commandLine.joinToString(separator = " ")}" }

        LauncherFrame.cleanup()
        if (!gameDir.exists()) gameDir.mkdirs()

        withContext(Dispatchers.IO) {
            val process = ProcessBuilder(commandLine)
                .directory(gameDir)
                .inheritIO()
                .start()
            Runtime.getRuntime().addShutdownHook(Thread { process.destroy() })
            process.waitFor()
        }

        return LaunchSuccess
    }
}