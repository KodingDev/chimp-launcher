@file:OptIn(ExperimentalSerializationApi::class, ObsoleteCoroutinesApi::class)

package dev.koding.launcher

import com.formdev.flatlaf.FlatDarkLaf
import dev.koding.launcher.auth.AuthManager
import dev.koding.launcher.data.assets.AssetIndex
import dev.koding.launcher.data.assets.toAsset
import dev.koding.launcher.data.config.ProfileConfig
import dev.koding.launcher.data.jdk.JdkFile
import dev.koding.launcher.data.jdk.JdkManifest
import dev.koding.launcher.data.launcher.LocalConfig
import dev.koding.launcher.data.launcher.RemoteConfig
import dev.koding.launcher.data.manifest.*
import dev.koding.launcher.data.runtime.JavaRuntime
import dev.koding.launcher.data.runtime.match
import dev.koding.launcher.data.runtime.select
import dev.koding.launcher.loader.ProfileLoader
import dev.koding.launcher.util.InputUtil
import dev.koding.launcher.util.replaceParams
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Taskbar
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.system.exitProcess

object Launcher {

    private val logger = KotlinLogging.logger {}

    val home = File(System.getProperty("user.home")).resolve(".chimp-launcher")

    init {
        if (System.getProperty("debug.log") != null) {
            Configurator.setAllLevels(LogManager.getRootLogger().name, Level.DEBUG)
        }
    }

    private fun downloadLibraries(manifest: LauncherManifest, folder: File): LibraryData {
        LauncherFrame.update("Downloading client jar", 0)
        logger.info { "Downloading client jar" }
        val clientJar =
            manifest.downloads?.client?.download(
                folder.resolve("net/minecraft/version/${manifest.id}/version-${manifest.id}.jar"),
                strict = true
            ) ?: error("No client jar")

        LauncherFrame.update("Downloading libraries", 0)
        logger.info { "Downloading libraries" }

        val libraries = manifest.libraries.filterMatchesRule()
        libraries.forEachIndexed { index, it ->
            LauncherFrame.updateProgress(index, libraries.size)
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
        LauncherFrame.update("Downloading asset index", 0)
        logger.info { "Downloading asset index" }
        val assetIndex = AssetIndex.load(
            manifest.assetIndex?.download(folder.resolve("indexes"))
                ?: error("No asset index")
        )

        LauncherFrame.update("Downloading assets", 0)
        logger.info { "Downloading assets" }

        var i = 0
        val context = newFixedThreadPoolContext(5, "Assets")
        assetIndex.objects.entries.map {
            CoroutineScope(context).async {
                it.toAsset().download(folder.resolve("objects"))
                LauncherFrame.updateProgress(i++, assetIndex.objects.size)
            }
        }.joinAll()
        return folder
    }

    private suspend fun downloadJava(manifest: LauncherManifest, root: File): File? {
        LauncherFrame.update("Downloading Java", 0)
        logger.info { "Downloading java" }

        val javaVersion = manifest.javaVersion ?: return null
        val home = root.resolve("java/${javaVersion.component}/${javaVersion.majorVersion}")

        val runtime = JavaRuntime.load().select()
        val runtimeData = runtime?.get(javaVersion.component)?.match(javaVersion) ?: return null

        val jdkManifest = JdkManifest.load(runtimeData.manifest.download(home))
        jdkManifest.files.entries.forEachIndexed { i, (path, data) ->
            LauncherFrame.updateProgress(i, jdkManifest.files.size)
            when (data.type) {
                JdkFile.Type.DIRECTORY -> {
                    val dir = home.resolve(path)
                    logger.debug { "Creating directory: $dir" }
                    if (!dir.exists()) dir.mkdirs()
                }
                JdkFile.Type.FILE -> {
                    val file = data.downloads?.raw?.download(home.resolve(path), strict = true) ?: return@forEachIndexed
                    if (data.executable == true) file.setExecutable(true)
                }
                JdkFile.Type.LINK -> {
                    val source = home.resolve(path).toPath()
                    val target = home.resolve(data.target ?: return@forEachIndexed).toPath()
                    logger.debug { "Creating symlink: $source -> $target" }

                    if (Files.isSymbolicLink(source) || Files.isSymbolicLink(target)) return@forEachIndexed
                    Files.createSymbolicLink(source, target)
                }
            }
        }

        return home
    }

    suspend fun launch(manifest: LauncherManifest, gameDir: File) {
        logger.info { "Launching version: ${manifest.id}" }

        val launcherHome = home.resolve("launcher")
        val (clientJar, libraryFolder) = downloadLibraries(manifest, launcherHome.resolve("libraries"))
        val assetsFolder = downloadAssets(manifest, launcherHome.resolve("assets"))
        val javaHome = downloadJava(manifest, launcherHome) ?: error("Failed to download Java")

        LauncherFrame.update("Authenticating", 0)
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
            *manifest.arguments.game.toFilteredArray(),
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

        LauncherFrame.cleanup()
        if (!gameDir.exists()) gameDir.mkdirs()

        withContext(Dispatchers.IO) {
            val process = ProcessBuilder(commandLine)
                .directory(gameDir)
                .inheritIO()
                .start()
            process.waitFor()
        }
    }

    data class LibraryData(
        val clientJar: File,
        val folder: File
    )
}

object LauncherFrame {
    private lateinit var frame: JFrame

    private val log = JTextArea().apply { isEditable = false }
    private val progress = JProgressBar()
    private val status = JLabel("Starting game...").apply { alignmentX = Component.CENTER_ALIGNMENT }

    fun create() {
        val image = ImageIO.read(LauncherFrame::class.java.getResourceAsStream("/assets/logo.png"))
        runCatching { Taskbar.getTaskbar().iconImage = image }

        frame = JFrame("Chimp Launcher")
        frame.iconImage = image
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isResizable = false
        frame.setSize(600, 400)
        frame.setLocationRelativeTo(null)

        val panel = JPanel().apply {
            layout = BorderLayout()

            // Log text area, current phase, and progress bar
            add(JScrollPane(log), BorderLayout.CENTER)
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

                add(status)
                add(Box.createVerticalStrut(10))
                add(progress)
            }, BorderLayout.SOUTH)
        }

        frame.contentPane = panel
        frame.isVisible = true
    }

    fun cleanup() {
        frame.isVisible = false
        frame.dispose()
    }

    fun update(status: String = this.status.text, progress: Int = this.progress.value) {
        this.progress.value = progress
        this.status.text = status
    }

    fun updateProgress(current: Int, max: Int) {
        this.progress.value = (current * 100.0 / max).toInt()
    }

    fun log(message: String) {
        log.append(message)
        log.caretPosition = log.document.length
    }
}

suspend fun main() {
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    System.setProperty("apple.awt.application.name", "Chimp Launcher")
    System.setProperty("apple.awt.application.appearance", "system")
    FlatDarkLaf.setup()
    LauncherFrame.create()
    LauncherFrame.update("Loading profiles")

    val config = RemoteConfig.fromUrl(LocalConfig.load().config)
    val choice = InputUtil.askSelection("Select a profile", *config.profiles.map { it.name }.toTypedArray())
        ?: exitProcess(0)

    val profile = config.profiles.firstOrNull { it.name == choice } ?: error("Invalid profile")
    val loader = ProfileLoader(ProfileConfig.fromUrl(profile.url))

    loader.load()
    loader.start()
}