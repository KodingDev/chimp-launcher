@file:OptIn(ExperimentalSerializationApi::class, ObsoleteCoroutinesApi::class)

package dev.koding.launcher

import dev.koding.launcher.data.launcher.LocalConfig
import dev.koding.launcher.data.launcher.ProfileConfig
import dev.koding.launcher.data.launcher.RemoteConfig
import dev.koding.launcher.launch.GameDirectory
import dev.koding.launcher.launch.LauncherDirectory
import dev.koding.launcher.launch.ResourcesDirectory
import dev.koding.launcher.loader.*
import dev.koding.launcher.loader.resolvers.MinecraftVersionResolver
import dev.koding.launcher.util.fromUrl
import dev.koding.launcher.util.json
import dev.koding.launcher.util.readResource
import dev.koding.launcher.util.system.SwingUtil
import dev.koding.launcher.util.system.configureLogging
import dev.koding.launcher.util.ui.applySwingTheme
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator
import java.io.File
import kotlin.system.exitProcess

private val resourceManager = ResourceManager {
    +MinecraftVersionResolver

    +NamedResource
    +UrlResource
    +FileResource
}

suspend fun main(args: Array<String>) {
    configureLogging()

    val parser = ArgParser("chimp-launcher")
    val profile by parser.option(
        ArgType.String,
        "profile",
        description = "The profile to use"
    )
    val debug by parser.option(
        ArgType.Boolean,
        "debug",
        description = "Enable debug mode"
    )
    parser.parse(args)

    if (debug == true) Configurator.setAllLevels(LogManager.getRootLogger().name, Level.DEBUG)
    if (profile != null) return launchProfile(File(profile!!).json(), resourceManager)
    launch()
}

private suspend fun launch() {
    applySwingTheme()
    LauncherFrame.create()
    LauncherFrame.update("Loading profiles")

    val configPath = System.getProperty("launcher.remoteConfig")?.let { File(it) }
    val remoteConfig = configPath?.json()
        ?: (readResource<LocalConfig>("/config.json")?.config?.fromUrl<RemoteConfig>() ?: return)
    val choice = SwingUtil.askSelection("Select a profile", *remoteConfig.profiles.map { it.name }.toTypedArray())
        ?: exitProcess(0)

    val profile = remoteConfig.profiles.firstOrNull { it.name == choice } ?: error("Invalid profile")
    val profileConfig = resourceManager.load(profile.resource)?.json<ProfileConfig>()
        ?: error("Failed to load config")

    launchProfile(profileConfig, resourceManager)
}

private suspend fun launchProfile(profile: ProfileConfig, resourceManager: ResourceManager) {
    val home = File(System.getProperty("user.home")).resolve(".chimp-launcher")
    val loader = profile.loader(resourceManager) {
        config[ResourcesDirectory] = home.resolve("resources")
        config[LauncherDirectory] = home.resolve("launcher")
        config[GameDirectory] = home.resolve("profiles/${profile.name}")

        progressHandler = { name, progress -> LauncherFrame.update(name, progress?.times(100)?.toInt()) }
    }

    loader.load()
    val process = loader.start() ?: return
    LauncherFrame.cleanup()
    withContext(Dispatchers.IO) { process.waitFor() }
    exitProcess(0)
}