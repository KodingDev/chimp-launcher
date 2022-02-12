package dev.koding.launcher.custom

import dev.koding.launcher.data.launcher.ProfileConfig
import dev.koding.launcher.data.launcher.download
import dev.koding.launcher.data.launcher.logger
import dev.koding.launcher.data.minecraft.manifest.Asset
import dev.koding.launcher.data.minecraft.manifest.LauncherManifest
import dev.koding.launcher.data.minecraft.manifest.Rule
import dev.koding.launcher.data.minecraft.manifest.asDownload
import dev.koding.launcher.frame.LauncherFrame
import dev.koding.launcher.launch.*
import dev.koding.launcher.launch.stages.DownloadLibraries
import dev.koding.launcher.launcherHome
import dev.koding.launcher.loader.loader
import dev.koding.launcher.resourceManager
import dev.koding.launcher.util.URISerializer
import dev.koding.launcher.util.json
import dev.koding.launcher.util.system.OS
import dev.koding.launcher.util.system.SwingUtil
import dev.koding.launcher.util.system.setLogLevel
import dev.koding.launcher.util.ui.content
import dev.koding.launcher.util.ui.dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.apache.logging.log4j.Level
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import kotlin.system.exitProcess

suspend fun main() {
    try {
        run()
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

suspend fun run() {
    setLogLevel(Level.DEBUG)
    LauncherFrame.init()

    val versionIndexUrl = when (SwingUtil.askSelection("Release or beta?", "Release", "Beta")) {
        "Release" -> "https://game-launcher.feathermc.com/release/version_index.json"
        "Beta" -> "https://game-launcher.feathermc.com/beta/version_index.json"
        else -> return
    }
    val versionIndex = resourceManager.load(URI(versionIndexUrl))?.json<VersionIndex>()
        ?: throw IllegalStateException("Failed to load version index")

    val selectedVersion =
        SwingUtil.askSelection("Select version", *versionIndex.metadata.map { it.name }.toTypedArray())
    val version = versionIndex.metadata.first { it.name == selectedVersion }
    val versionManifest = resourceManager.load(URI(version.info.url))
        ?.json<FeatherClientManifest>() ?: throw IllegalStateException("Failed to load version")

    val minecraftManifest = LauncherManifest(
        id = versionManifest.id,
        mainClass = versionManifest.mainClass,
        minecraftArguments = versionManifest.minecraftArguments + " --assetsDir \${assets_root}",
        type = "release",
        javaVersion = LauncherManifest.JavaVersion(
            "Feather-${version.name}",
            1,
            asset = versionManifest.jre[OS.name]?.toAsset()
        ),
        libraries = versionManifest.libraries.filter { it.classpath }.map {
            LauncherManifest.Library(
                name = it.name ?: "",
                url = it.url.toString(),
                downloads = LauncherManifest.Library.Downloads(it.toAsset()),
                rules = it.rules
            )
        } + versionManifest.nativeLibraries.mapIndexedNotNull { i, it ->
            val download = it.natives[OS.name] ?: return@mapIndexedNotNull null
            LauncherManifest.Library(
                name = "native-$i",
                url = it.url.toString(),
                downloads = LauncherManifest.Library.Downloads(download.toAsset()),
                rules = it.rules,
                native = true
            )
        },
        assets = versionManifest.gameAssetsIndex.name?.removeSuffix(".json"),
        assetIndex = versionManifest.gameAssetsIndex.toAsset(),
        downloads = LauncherManifest.Downloads(
            client = versionManifest.libraries[0].toAsset()
        ),
    )

    val minecraftManifestFile =
        resourceManager.config[ResourcesDirectory]?.resolve("feather/versions/${minecraftManifest.id}/minecraft.json")
            ?: throw IllegalStateException("Failed to resolve minecraft manifest file")
    minecraftManifestFile.parentFile.mkdirs()
    minecraftManifestFile.writeText(json.encodeToString(minecraftManifest))

    val profile = ProfileConfig("feather-${version.name}", ProfileConfig.Launch(minecraftManifestFile.toURI()))

    val loader = profile.loader(resourceManager) {
        config[LauncherDirectory] = launcherHome.resolve("launcher")
        config[GameDirectory] = launcherHome.resolve("profiles/feather/${profile.name}")
        config[StartupHandler] = { launcher ->
            // Crack the JAR
            val libraryFolder =
                runBlocking { launcher.get<DownloadLibraries.Result>(DownloadLibraries) }?.librariesFolder
            libraryFolder?.walkTopDown()?.forEach {
                if (!it.path.contains("net${File.separator}digitalingot") || it.isDirectory) return@forEach
                it.crack()
            }

            // Move the mods
            val modsFolder = config[GameDirectory]?.resolve("mods") ?: error("Failed to resolve mods folder")
            modsFolder.deleteRecursively()
            versionManifest.libraries.filter { !it.classpath }.forEach {
                logger.debug { "Downloading mod ${it.name} to mods folder" }
                val filename = it.name?.substringAfterLast('/') ?: return@forEach
                val file =
                    it.toAsset().asDownload()?.download(modsFolder.resolve(filename), strict = true) ?: return@forEach
                file.crack()
            }
        }

        progressHandler = { name, progress -> LauncherFrame.main?.update(name, progress?.times(100)?.toInt()) }
    }

    loader.load()
    val process = loader.start() ?: return
    withContext(Dispatchers.IO) { process.waitFor() }
    exitProcess(0)
}

@Serializable
data class VersionIndex(
    val metadata: List<Version>
) {
    @Serializable
    data class Version(
        val name: String,
        val info: Info
    ) {
        @Serializable
        data class Info(
            val url: String
        )
    }
}

@Serializable
data class FeatherClientManifest(
    @SerialName("assets_index") val featherAssetsIndex: FeatherDownload,
    @SerialName("game_assets_index") val gameAssetsIndex: FeatherDownload,
    @SerialName("jre") val jre: Map<String, FeatherDownload>,
    @SerialName("libraries") val libraries: List<FeatherDownload>,
    @SerialName("main_class") val mainClass: String,
    @SerialName("minecraft_arguments") val minecraftArguments: String,
    @SerialName("name") val id: String,
    @SerialName("native_libraries") val nativeLibraries: List<FeatherDownload>,
)

@Serializable
data class FeatherDownload(
    @Serializable(with = URISerializer::class)
    val url: URI? = null,
    val name: String? = null,
    val sha1: String? = null,
    val rules: List<Rule> = emptyList(),
    val natives: Map<String, FeatherDownload> = mapOf(),
    val classpath: Boolean = true
) {
    fun toAsset() = Asset(url, path = "feather/$name", sha1 = sha1)
}

fun File.crack() {
    val bannedMethods = setOf("func_71400_g", "method_1592", "m_91395_")
    val files = ConcurrentHashMap<String, ByteArray>()
    logger.info { "Cracking $name" }

    JarFile(this).use { jar ->
        jar.entries().iterator().forEach {
            files[it.name] = jar.getInputStream(it).readBytes()
        }
    }

    var modified = false
    files.filter { it.key.endsWith(".class") }.forEach { (name, bytes) ->
        val cr = ClassReader(bytes)
        val clazz = ClassNode()
        cr.accept(clazz, 0)

        clazz.methods.forEach { method ->
            for (insn in method.instructions) {
                if (insn is MethodInsnNode && bannedMethods.contains(insn.name)) {
                    logger.info { "Replaced an instruction at ${clazz.name}.${method.name}${method.desc}" }
                    method.instructions.set(insn, InsnNode(Opcodes.POP))
                    modified = true
                }
            }
        }

        val cw = ClassWriter(0)
        clazz.accept(cw)
        files[name] = cw.toByteArray()
    }

    if (modified) {
        logger.info { "Cracked. Saving..." }
        ZipOutputStream(outputStream()).use { zip ->
            zip.setLevel(9)
            files.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
            }
        }
        logger.info { "Saved." }
    }
}