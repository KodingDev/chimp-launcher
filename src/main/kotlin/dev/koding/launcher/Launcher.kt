@file:OptIn(ExperimentalSerializationApi::class, ObsoleteCoroutinesApi::class)

package dev.koding.launcher

import dev.koding.launcher.auth.AuthManager
import dev.koding.launcher.data.assets.AssetIndex
import dev.koding.launcher.data.assets.toAsset
import dev.koding.launcher.data.config.ProfileConfig
import dev.koding.launcher.data.jdk.JdkFile
import dev.koding.launcher.data.jdk.JdkManifest
import dev.koding.launcher.data.local.LocalConfig
import dev.koding.launcher.data.manifest.*
import dev.koding.launcher.data.runtime.JavaRuntime
import dev.koding.launcher.data.runtime.match
import dev.koding.launcher.data.runtime.select
import dev.koding.launcher.loader.ProfileLoader
import dev.koding.launcher.util.replaceParams
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator
import java.io.File
import java.nio.file.Files

object Launcher {

    private val logger = KotlinLogging.logger {}

    val home = File(System.getProperty("user.home")).resolve(".chimp-launcher")

    init {
        if (System.getProperty("debug.log") != null) {
            Configurator.setAllLevels(LogManager.getRootLogger().name, Level.DEBUG)
        }
    }

    private fun downloadLibraries(manifest: LauncherManifest, folder: File): LibraryData {
        logger.info { "Downloading client jar" }
        val clientJar =
            manifest.downloads?.client?.download(
                folder.resolve("net/minecraft/version/${manifest.id}/version-${manifest.id}.jar"),
                strict = true
            ) ?: error("No client jar")

        logger.info { "Downloading libraries" }
        manifest.libraries.filterMatchesRule().forEach {
            it.asset?.download(folder)
            it.downloads?.classifiers?.let { classifiers ->
                classifiers.linuxNatives?.download(folder)
                classifiers.macosNatives?.download(folder)
                classifiers.windowsNatives?.download(folder)
            }
        }

        return LibraryData(clientJar, folder)
    }

    private suspend fun downloadAssets(manifest: LauncherManifest, folder: File): File {
        logger.info { "Downloading asset index" }
        val assetIndex = AssetIndex.load(
            manifest.assetIndex?.download(folder.resolve("indexes"))
                ?: error("No asset index")
        )

        logger.info { "Downloading assets" }

        val context = newFixedThreadPoolContext(5, "Assets")
        assetIndex.objects.entries.map {
            CoroutineScope(context).async {
                it.toAsset().download(folder.resolve("objects"))
            }
        }.joinAll()
        return folder
    }

    suspend fun launch(manifest: LauncherManifest, gameDir: File) {
        logger.info { "Launching version: ${manifest.id}" }

        val launcherHome = home.resolve("launcher")
        val (clientJar, libraryFolder) = downloadLibraries(manifest, launcherHome.resolve("libraries"))
        val assetsFolder = downloadAssets(manifest, launcherHome.resolve("assets"))
        val javaHome = downloadJava(manifest, launcherHome) ?: error("Failed to download Java")

        logger.info { "Authenticating" }
        val auth = AuthManager(launcherHome.resolve("auth")).login()

        val classpath = listOf(
            *manifest.libraries.filterMatchesRule()
                .flatMap { it.assets }
                .map { libraryFolder.resolve(it.path ?: "").absolutePath }
                .toTypedArray(),
            clientJar.absolutePath
        ).joinToString(separator = ":")

        val commandLine = listOf(
            getJavaPath(javaHome).absolutePath ?: error("No Java version"),
            *manifest.arguments.jvm.toFilteredArray(),
            manifest.mainClass,
            *manifest.arguments.game.toFilteredArray()
        ).map {
            it.replaceParams(
                "natives_directory" to ".",
                "launcher_name" to "chimp-launcher",
                "launcher_version" to "1.0.0",
                "classpath" to classpath,

                "auth_player_name" to auth.profile.name,
                "version_name" to manifest.id,
                "game_directory" to gameDir,
                "assets_root" to assetsFolder.absolutePath,
                "assets_index_name" to (manifest.assets ?: error("No assets index")),
                "auth_uuid" to auth.profile.id,
                "auth_access_token" to auth.token.accessToken,
                "user_type" to "mojang",
                "version_type" to manifest.type
            )
        }

        logger.info { "Launching Minecraft" }
        logger.debug { "Command line: ${commandLine.joinToString(separator = " ")}" }

        if (!gameDir.exists()) gameDir.mkdirs()
        withContext(Dispatchers.IO) {
            val process = ProcessBuilder(commandLine)
                .directory(gameDir)
                .inheritIO()
                .start()
            process.waitFor()
        }
    }

    private suspend fun downloadJava(manifest: LauncherManifest, root: File): File? {
        logger.info { "Downloading java" }
        val javaVersion = manifest.javaVersion ?: return null
        val home = root.resolve("java/${javaVersion.component}/${javaVersion.majorVersion}")

        val runtime = JavaRuntime.load().select()
        val runtimeData = runtime?.get(javaVersion.component)?.match(javaVersion) ?: return null

        val jdkManifest = JdkManifest.load(runtimeData.manifest.download(home))
        jdkManifest.files.forEach { (path, data) ->
            when (data.type) {
                JdkFile.Type.DIRECTORY -> {
                    val dir = home.resolve(path)
                    logger.debug { "Creating directory: $dir" }
                    if (!dir.exists()) dir.mkdirs()
                }
                JdkFile.Type.FILE -> {
                    val file = data.downloads?.raw?.download(home.resolve(path), strict = true) ?: return@forEach
                    if (data.executable == true) file.setExecutable(true)
                }
                JdkFile.Type.LINK -> {
                    val source = home.resolve(path).toPath()
                    val target = home.resolve(data.target ?: return@forEach).toPath()
                    logger.debug { "Creating symlink: $source -> $target" }

                    if (Files.isSymbolicLink(source) || Files.isSymbolicLink(target)) return@forEach
                    Files.createSymbolicLink(source, target)
                }
            }
        }

        return home
    }

    data class LibraryData(
        val clientJar: File,
        val folder: File
    )
}


suspend fun main() {
    val config = LocalConfig.load()
    val loader = ProfileLoader(ProfileConfig.fromUrl(config.profile))
    loader.load()
    loader.start()
}