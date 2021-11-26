@file:OptIn(ExperimentalSerializationApi::class, ObsoleteCoroutinesApi::class)

package dev.koding.launcher

import dev.koding.launcher.data.launcher.LocalConfig
import dev.koding.launcher.data.launcher.ProfileConfig
import dev.koding.launcher.data.launcher.RemoteConfig
import dev.koding.launcher.data.minecraft.manifest.LauncherManifest
import dev.koding.launcher.launch.*
import dev.koding.launcher.loader.*
import dev.koding.launcher.loader.resolvers.MinecraftVersionResolver
import dev.koding.launcher.loader.resolvers.ModrinthResolver
import dev.koding.launcher.util.*
import dev.koding.launcher.util.system.SwingUtil
import dev.koding.launcher.util.system.configureLogging
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

private val home = File(System.getProperty("user.home")).resolve(".chimp-launcher")
private val resourceManager = ResourceManager {
    +MinecraftVersionResolver
    +ModrinthResolver

    +NamedResource
    +UrlResource
    +FileResource

    config[ResourcesDirectory] = home.resolve("resources")
}

suspend fun main(args: Array<String>) {
    configureLogging()

    val parser = ArgParser("chimp-launcher")
    val profile by parser.option(ArgType.String, "profile", description = "The profile to use")
    val version by parser.option(ArgType.String, "version", description = "A Minecraft version to run")
    val debug by parser.option(ArgType.Boolean, "debug", description = "Enable debug mode")
    val gui by parser.option(ArgType.Boolean, "gui", description = "Enable the GUI")
    val gameDirectory by parser.option(
        ArgType.String,
        "game-directory",
        description = "The directory to use for the game"
    )
    val launcherDirectory by parser.option(
        ArgType.String,
        "launcher-directory",
        description = "The directory to use for the launcher (assets, libraries)"
    )
    val arguments by parser.option(ListArgument, "args", description = "Arguments to pass to the game")
    val jvm by parser.option(ListArgument, "jvm", description = "Arguments to pass to the JVM")
    val vanilla by parser.option(ArgType.String, "vanilla", description = "Start a version from the vanilla launcher")
    parser.parse(args)

    val vanillaHome = minecraftHome.takeIf { vanilla != null }
    val launchOptions = LaunchOptions(
        vanillaHome ?: gameDirectory?.let { File(it) }?.takeIf { it.exists() },
        vanillaHome ?: launcherDirectory?.let { File(it) }?.takeIf { it.exists() },
        LauncherManifest.Arguments(
            arguments?.map { LauncherManifest.Arguments.Argument(it) } ?: emptyList(),
            jvm?.map { LauncherManifest.Arguments.Argument(it) } ?: emptyList()
        )
    )

    if (gui == true) LauncherFrame.create()
    if (debug == true) Configurator.setAllLevels(LogManager.getRootLogger().name, Level.DEBUG)
    if (vanilla != null) return launchVanilla(vanilla!!)
    if (version != null) return launchProfile(
        ProfileConfig(version!!, ProfileConfig.Launch("profile:${version}")),
        launchOptions
    )
    if (profile != null) return launchProfile(File(profile!!).json(), launchOptions)

    launch()
}

private suspend fun launch() {
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

    launchProfile(profileConfig)
}

private suspend fun launchVanilla(name: String) {
    val vanillaHome = minecraftHome.takeIf { it.exists() }
        ?: error("Vanilla home not found")

    val launchOptions = LaunchOptions(vanillaHome, vanillaHome) {
        config[StartJarPath] = vanillaHome.resolve("versions/${name}/${name}.jar")
    }

    val profileConfig = ProfileConfig(
        name, ProfileConfig.Launch("profile:vanilla"), resources = listOf(
            FileResource("profile:vanilla", vanillaHome.resolve("versions/${name}/${name}.json").absolutePath)
        )
    )

    launchProfile(profileConfig, launchOptions)
}

private suspend fun launchProfile(profile: ProfileConfig, options: LaunchOptions = LaunchOptions()) {
    val loader = profile.loader(resourceManager) {
        config[ExtraArgs] = options.arguments
        config[LauncherDirectory] = options.launcherDirectory ?: home.resolve("launcher")
        config[GameDirectory] = options.gameDirectory ?: home.resolve("profiles/${profile.name}")

        progressHandler = { name, progress -> LauncherFrame.update(name, progress?.times(100)?.toInt()) }
    }.apply(options.block)

    loader.load()
    val process = loader.start() ?: return
    LauncherFrame.cleanup()
    withContext(Dispatchers.IO) { process.waitFor() }
    exitProcess(0)
}

data class LaunchOptions(
    val gameDirectory: File? = null,
    val launcherDirectory: File? = null,
    val arguments: LauncherManifest.Arguments = LauncherManifest.Arguments(),
    val block: ProfileLoader.() -> Unit = {}
)