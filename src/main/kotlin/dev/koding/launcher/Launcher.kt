@file:OptIn(ExperimentalSerializationApi::class, ObsoleteCoroutinesApi::class)

package dev.koding.launcher

import dev.koding.launcher.data.launcher.LocalConfig
import dev.koding.launcher.data.launcher.ProfileConfig
import dev.koding.launcher.data.launcher.RemoteConfig
import dev.koding.launcher.data.minecraft.manifest.LauncherManifest
import dev.koding.launcher.data.minecraft.manifest.toArguments
import dev.koding.launcher.launch.*
import dev.koding.launcher.loader.*
import dev.koding.launcher.loader.resolvers.FabricResolver
import dev.koding.launcher.loader.resolvers.MinecraftVersionResolver
import dev.koding.launcher.loader.resolvers.ModrinthResolver
import dev.koding.launcher.util.*
import dev.koding.launcher.util.system.SwingUtil
import dev.koding.launcher.util.system.configureLogging
import dev.koding.launcher.util.system.setLogLevel
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import org.apache.logging.log4j.Level
import java.io.File
import kotlin.system.exitProcess

// TODO: Clean up this class *somehow*
private val home = File(System.getProperty("user.home")).resolve(".chimp-launcher")
private val resourceManager = ResourceManager {
    +MinecraftVersionResolver
    +ModrinthResolver
    +FabricResolver

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
            arguments?.toArguments() ?: emptyList(),
            jvm?.toArguments() ?: emptyList()
        )
    )

    if (gui == true) LauncherFrame.create()
    if (debug == true) setLogLevel(Level.DEBUG)
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

    val localConfig = readResource<LocalConfig>("/config.json")
    val configPath = System.getProperty("launcher.remoteConfig")?.let { File(it) }
    val remote = configPath?.json<RemoteConfig>() ?: localConfig?.config?.fromUrl()

    val profileConfig = localConfig?.profile?.fromUrl<ProfileConfig>() ?: localConfig?.let {
        val choice = SwingUtil.askSelection(
            "Select a profile",
            *(remote?.profiles?.map { it.name }?.toTypedArray() ?: emptyArray())
        ) ?: exitProcess(0)

        val profile = remote?.profiles?.firstOrNull { it.name == choice } ?: error("Invalid profile")
        resourceManager.load(profile.resource)?.json<ProfileConfig>() ?: error("Failed to load config")
    } ?: error("Failed to load config")
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
    if (profile.launch.debug) setLogLevel(Level.DEBUG)
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