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
import dev.koding.launcher.util.ui.content
import dev.koding.launcher.util.ui.frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import java.awt.BorderLayout
import java.awt.Component
import java.io.File
import javax.swing.*
import kotlin.system.exitProcess

object LauncherFrame {
    private var frame: JFrame? = null

    private val log = JTextArea().apply { isEditable = false }
    private val progress = JProgressBar()
    private val status = JLabel("Starting game...").apply { alignmentX = Component.CENTER_ALIGNMENT }

    fun create() {
        frame = frame(size = 600 to 400) {
            content {
                layout = BorderLayout()

                JScrollPane(log) + BorderLayout.CENTER
                panel {
                    padding = 10

                    +status
                    +verticalSpace(10)
                    +progress
                } + BorderLayout.SOUTH
            }
        }
    }

    fun cleanup() {
        frame ?: return
        frame!!.isVisible = false
        frame!!.dispose()
    }

    fun update(status: String? = null, progress: Int? = null) {
        progress?.let { this.progress.value = it }
        status?.let { this.status.text = it }
    }

    fun log(message: String) {
        log.append(message)
        log.caretPosition = log.document.length
    }
}

suspend fun main() {
    configureLogging()
    applySwingTheme()

    LauncherFrame.create()
    LauncherFrame.update("Loading profiles")

    val resourceManager = ResourceManager {
        +MinecraftVersionResolver

        +NamedResource
        +UrlResource
        +FileResource
    }

    val configPath = System.getProperty("launcher.remoteConfig")?.let { File(it) }
    val remoteConfig = configPath?.json()
        ?: (readResource<LocalConfig>("/config.json")?.config?.fromUrl<RemoteConfig>() ?: return)
    val choice = SwingUtil.askSelection("Select a profile", *remoteConfig.profiles.map { it.name }.toTypedArray())
        ?: exitProcess(0)

    val profile = remoteConfig.profiles.firstOrNull { it.name == choice } ?: error("Invalid profile")
    val profileConfig = resourceManager.load(profile.resource)?.json<ProfileConfig>()
        ?: error("Failed to load config")

    val home = File(System.getProperty("user.home")).resolve(".chimp-launcher")
    val loader = profileConfig.loader(resourceManager) {
        config[ResourcesDirectory] = home.resolve("resources")
        config[LauncherDirectory] = home.resolve("launcher")
        config[GameDirectory] = home.resolve("profiles/${profileConfig.name}")

        progressHandler = { name, progress -> LauncherFrame.update(name, progress?.times(100)?.toInt()) }
    }

    loader.load()

    val process = loader.start() ?: return
    LauncherFrame.cleanup()
    withContext(Dispatchers.IO) { process.waitFor() }
    exitProcess(0)
}