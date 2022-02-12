@file:OptIn(ExperimentalSerializationApi::class, ObsoleteCoroutinesApi::class)

package dev.koding.launcher

import dev.koding.launcher.data.launcher.LocalConfig
import dev.koding.launcher.data.launcher.ProfileConfig
import dev.koding.launcher.data.launcher.RemoteConfig
import dev.koding.launcher.data.minecraft.manifest.LauncherManifest
import dev.koding.launcher.data.minecraft.manifest.toArguments
import dev.koding.launcher.frame.LauncherFrame
import dev.koding.launcher.launch.*
import dev.koding.launcher.loader.ProfileLoader
import dev.koding.launcher.loader.ResourceManager
import dev.koding.launcher.loader.loader
import dev.koding.launcher.loader.resolvers.FabricResolver
import dev.koding.launcher.loader.resolvers.MinecraftVersionResolver
import dev.koding.launcher.loader.resolvers.ModrinthResolver
import dev.koding.launcher.util.*
import dev.koding.launcher.util.system.MacUtil
import dev.koding.launcher.util.system.SwingUtil
import dev.koding.launcher.util.system.configureLogging
import dev.koding.launcher.util.system.setLogLevel
import dev.koding.launcher.util.ui.content
import dev.koding.launcher.util.ui.dialog
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import org.apache.logging.log4j.Level
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.net.URI
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import kotlin.system.exitProcess

// TODO: Clean up this class *somehow*
val arguments = arrayListOf<String>()

val resourceManager = ResourceManager {
    +FabricResolver
    +MinecraftVersionResolver
    +ModrinthResolver

    config[ResourcesDirectory] = launcherHome.resolve("resources")
}

suspend fun main(args: Array<String>) {
    // TODO: Check if Java is 64-bit

    arguments.addAll(args)
    configureLogging()

    val parser = ArgParser("chimp-launcher")
    val profilePath by parser.option(ArgType.String, "profile-path", description = "A path to the profile to use")
    val profile by parser.option(ArgType.String, "profile", description = "The profile to use")
    val version by parser.option(ArgType.String, "version", description = "A Minecraft version to run")
    val debug by parser.option(ArgType.Boolean, "debug", description = "Enable debug log mode")
    val trace by parser.option(ArgType.Boolean, "trace", description = "Enable trace log mode")
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

    if (gui == true) LauncherFrame.init()
    if (debug == true) setLogLevel(Level.DEBUG)
    if (trace == true) setLogLevel(Level.TRACE)

    try {
        if (vanilla != null) return launchVanilla(vanilla!!)
        if (version != null) return launchProfile(
            ProfileConfig(version!!, ProfileConfig.Launch(URI("content://net.minecraft/$version"))),
            launchOptions
        )
        if (profilePath != null) return launchProfile(File(profilePath!!).json(), launchOptions)
        launch(profile)
    } catch (e: Exception) {
        dialog(title = "Error") {
            content {
                layout = BorderLayout()

                panel {
                    padding = 10

                    +JLabel("Uh oh! Something went wrong!").apply {
                        foreground = Color(0xef5350)
                        font = font.deriveFont(font.style or Font.BOLD)
                    }

                    +verticalSpace(10)
                    +JLabel("Please report this error to the developers:")
                } + BorderLayout.NORTH

                JScrollPane(JTextArea().apply {
                    isEditable = false
                    text = e.stackTraceToString()
                }).apply {
                    preferredSize = Dimension(600, 400)
                } + BorderLayout.SOUTH
            }

            pack()
            requestFocusInWindow()
            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) = exitProcess(0)
            })
        }
    }
}

private suspend fun launch(selectedProfile: String?) {
    LauncherFrame.init()
    LauncherFrame.main?.update("Loading profiles")

    val localConfig = readResource<LocalConfig>("/config.json")
    val configPath = System.getProperty("launcher.remoteConfig")?.let { File(it) }
    val remote = configPath?.json<RemoteConfig>() ?: localConfig?.config?.fromUrl()

    var name = selectedProfile
    val profileConfig = localConfig?.profile?.fromUrl<ProfileConfig>() ?: localConfig?.let {
        val choice = selectedProfile ?: SwingUtil.askSelection(
            "Select a profile",
            *(remote?.profiles?.map { it.name }?.toTypedArray() ?: emptyArray())
        ) ?: exitProcess(0)

        val profile = remote?.profiles?.firstOrNull { it.name == choice } ?: error("Invalid profile")
        name = profile.name
        resourceManager.load(profile.resource)?.json<ProfileConfig>() ?: error("Failed to load config")
    } ?: error("Failed to load config")

    launchProfile(profileConfig, options = LaunchOptions(profile = name))
}

private suspend fun launchVanilla(name: String) {
    val vanillaHome = minecraftHome.takeIf { it.exists() }
        ?: error("Vanilla home not found")

    val launchOptions = LaunchOptions(vanillaHome, vanillaHome) {
        config[StartJarPath] = vanillaHome.resolve("versions/${name}/${name}.jar")
    }

    val profileConfig =
        ProfileConfig(name, ProfileConfig.Launch(vanillaHome.resolve("versions/${name}/${name}.json").toURI()))
    launchProfile(profileConfig, launchOptions)
}

private suspend fun launchProfile(profile: ProfileConfig, options: LaunchOptions = LaunchOptions()) {
    if (profile.launch.debug) setLogLevel(Level.DEBUG)
    if (profile.launch.macOSWorkaround)
        MacUtil.runWorkaround(*(options.profile?.let { arrayOf("--profile", it) } ?: emptyArray()))

    val loader = profile.loader(resourceManager) {
        config[ExtraArgs] = options.arguments
        config[LauncherDirectory] = options.launcherDirectory ?: launcherHome.resolve("launcher")
        config[GameDirectory] = options.gameDirectory ?: launcherHome.resolve("profiles/${profile.name}")

        progressHandler = { name, progress -> LauncherFrame.main?.update(name, progress?.times(100)?.toInt()) }
    }.apply(options.block)

    loader.load()
    val process = loader.start() ?: return
    LauncherFrame.main?.dispose()
    withContext(Dispatchers.IO) { process.waitFor() }
    exitProcess(0)
}

data class LaunchOptions(
    val gameDirectory: File? = null,
    val launcherDirectory: File? = null,
    val arguments: LauncherManifest.Arguments = LauncherManifest.Arguments(),
    val profile: String? = null,
    val block: ProfileLoader.() -> Unit = {}
)