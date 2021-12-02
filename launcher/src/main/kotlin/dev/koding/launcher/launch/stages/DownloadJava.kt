package dev.koding.launcher.launch.stages

import dev.koding.launcher.data.java.jdk.JdkManifest
import dev.koding.launcher.data.java.runtime.JavaRuntime
import dev.koding.launcher.data.java.runtime.match
import dev.koding.launcher.data.java.runtime.select
import dev.koding.launcher.data.launcher.download
import dev.koding.launcher.data.minecraft.manifest.asDownload
import dev.koding.launcher.launch.JavaDirectory
import dev.koding.launcher.launch.LaunchStage
import dev.koding.launcher.launch.LauncherDirectory
import dev.koding.launcher.launch.MinecraftLauncher
import dev.koding.launcher.util.json
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files

object DownloadJava : LaunchStage<DownloadJava.Result> {

    private val logger = KotlinLogging.logger {}

    override suspend fun run(launcher: MinecraftLauncher): Result {
        launcher.progressHandler("Downloading Java", 0.0)
        logger.info { "Downloading java" }

        val root = launcher.config[JavaDirectory]
            ?: launcher.config[LauncherDirectory]?.resolve("java")
            ?: error("Launcher directory not specified")
        val javaVersion = launcher.manifest.javaVersion ?: error("Java version does not exist")
        val home = root.resolve("${javaVersion.component}/${javaVersion.majorVersion}")

        val runtime = JavaRuntime.fetch().select()
        val runtimeData =
            runtime?.get(javaVersion.component)?.match(javaVersion) ?: error("No applicable Java version found")

        val jdkManifest =
            runtimeData.manifest.asDownload()?.download(home)?.json<JdkManifest>() ?: error("Invalid manifest")
        jdkManifest.files.entries.forEachIndexed { i, (path, data) ->
            launcher.progressHandler(null, i / jdkManifest.files.size.toDouble())
            when (data.type) {
                JdkManifest.File.Type.DIRECTORY -> {
                    val dir = home.resolve(path)
                    logger.debug { "Creating directory: $dir" }
                    if (!dir.exists()) dir.mkdirs()
                }
                JdkManifest.File.Type.FILE -> {
                    val file = data.downloads?.raw?.asDownload()?.download(home.resolve(path), strict = true)
                        ?: return@forEachIndexed
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

        return Result(home)
    }

    data class Result(
        val javaHome: File
    )
}