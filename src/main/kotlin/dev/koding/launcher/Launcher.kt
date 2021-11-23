@file:OptIn(ExperimentalSerializationApi::class, ObsoleteCoroutinesApi::class)

package dev.koding.launcher

import dev.koding.launcher.auth.AuthManager
import dev.koding.launcher.data.java.jdk.JdkManifest
import dev.koding.launcher.data.java.runtime.JavaRuntime
import dev.koding.launcher.data.java.runtime.match
import dev.koding.launcher.data.java.runtime.select
import dev.koding.launcher.data.launcher.LocalConfig
import dev.koding.launcher.data.launcher.RemoteConfig
import dev.koding.launcher.data.minecraft.assets.AssetIndex
import dev.koding.launcher.data.minecraft.assets.toAsset
import dev.koding.launcher.data.minecraft.manifest.*
import dev.koding.launcher.loader.ProfileLoader
import dev.koding.launcher.util.fromUrl
import dev.koding.launcher.util.json
import dev.koding.launcher.util.readResource
import dev.koding.launcher.util.replaceParams
import dev.koding.launcher.util.system.SwingUtil
import dev.koding.launcher.util.system.extractZip
import dev.koding.launcher.util.ui.applySwingTheme
import dev.koding.launcher.util.ui.content
import dev.koding.launcher.util.ui.frame
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator
import java.awt.BorderLayout
import java.awt.Component
import java.io.File
import java.nio.file.Files
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
            it.assets.forEach { it.download(folder) }
        }

        return LibraryData(clientJar, folder)
    }

    private suspend fun downloadAssets(manifest: LauncherManifest, folder: File): File {
        LauncherFrame.update("Downloading asset index", 0)
        logger.info { "Downloading asset index" }
        val assetIndex = manifest.assetIndex?.download(folder.resolve("indexes"))?.json<AssetIndex>()
            ?: error("No asset index")

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

        val runtime = JavaRuntime.fetch().select()
        val runtimeData = runtime?.get(javaVersion.component)?.match(javaVersion) ?: return null

        val jdkManifest = runtimeData.manifest.download(home).json<JdkManifest>()
        jdkManifest.files.entries.forEachIndexed { i, (path, data) ->
            LauncherFrame.updateProgress(i, jdkManifest.files.size)
            when (data.type) {
                JdkManifest.File.Type.DIRECTORY -> {
                    val dir = home.resolve(path)
                    logger.debug { "Creating directory: $dir" }
                    if (!dir.exists()) dir.mkdirs()
                }
                JdkManifest.File.Type.FILE -> {
                    val file = data.downloads?.raw?.download(home.resolve(path), strict = true) ?: return@forEachIndexed
                    if (data.executable == true) file.setExecutable(true)
                }
                JdkManifest.File.Type.LINK -> {
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

    private fun setupNatives(manifest: LauncherManifest, libraryFolder: File, root: File): File {
        logger.info { "Setting up natives" }
        val natives = manifest.libraries.filterMatchesRule().mapNotNull { it.native }

        if (root.exists()) root.deleteRecursively()
        root.mkdirs()

        natives.forEach {
            logger.debug { "Extracting natives: ${it.path} -> ${root.absolutePath}" }
            it.getLocation(libraryFolder).extractZip(root)
        }

        return root
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

        val nativesFolder =
            setupNatives(manifest, libraryFolder, libraryFolder.resolve("net/minecraft/natives/${manifest.id}"))
        val classpath = listOf(
            *manifest.libraries.filterMatchesRule()
                .flatMap { it.assets }
                .map { libraryFolder.resolve(it.path ?: "").absolutePath }
                .toTypedArray(),
            clientJar.absolutePath
        ).joinToString(separator = File.pathSeparator)

        val commandLine = listOf(
            getJavaPath(javaHome).absolutePath ?: error("No Java version"),
            *manifest.arguments?.jvm?.toFilteredArray()?.takeUnless { it.isEmpty() }
                ?: arrayOf("-Djava.library.path=\${natives_directory}", "-cp", "\${classpath}"),
            manifest.mainClass,
            *(manifest.arguments?.game?.toFilteredArray()?.takeUnless { it.isEmpty() }
                ?: manifest.minecraftArguments?.split(" ")?.toTypedArray()
                ?: emptyArray()),
        ).map {
            it.replaceParams(
                "natives_directory" to nativesFolder.absolutePath,
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
                "version_type" to manifest.type,
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
        exitProcess(0)
    }

    data class LibraryData(
        val clientJar: File,
        val folder: File
    )
}

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
    applySwingTheme()
    LauncherFrame.create()
    LauncherFrame.update("Loading profiles")

    val config = readResource<LocalConfig>("/config.json")?.config?.fromUrl<RemoteConfig>() ?: return
    val choice = SwingUtil.askSelection("Select a profile", *config.profiles.map { it.name }.toTypedArray())
        ?: exitProcess(0)

    val profile = config.profiles.firstOrNull { it.name == choice } ?: error("Invalid profile")
    val loader = ProfileLoader(profile.url.fromUrl())

    loader.load()
    loader.start()
}