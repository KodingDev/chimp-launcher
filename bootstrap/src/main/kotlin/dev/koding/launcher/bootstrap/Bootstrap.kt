package dev.koding.launcher.bootstrap

import dev.koding.launcher.data.launcher.Config
import dev.koding.launcher.data.launcher.Manifest
import dev.koding.launcher.data.launcher.download
import dev.koding.launcher.frame.LauncherFrame
import dev.koding.launcher.launcherHome
import java.io.File
import java.net.URLClassLoader


object Bootstrap {
    fun bootstrap(manifest: Manifest, args: Array<String>) {
        val frame = LauncherFrame(showLog = false, size = 600 to 85)
        frame.update("Bootstrapper is loading...")

        val libDir = launcherHome.resolve("launcher/libraries")
        val dependencies = manifest.dependencies.map { it.toDownload() }
            .map {
                it.download(libDir, progressHandler =
                { status, _ -> frame.update(status = status) }).toURI().toURL()
            }

        frame.dispose()
        val classLoader = URLClassLoader(dependencies.toTypedArray(), null)
        val launcher = classLoader.loadClass(manifest.main)
        System.setProperty("chimp.mainClass", "dev.koding.launcher.bootstrap.BootstrapKt")
        launcher.getMethod("main", Array<String>::class.java).invoke(null, args)
    }
}

fun main(args: Array<String>) {
    val config: Manifest = System.getProperty("launcher.config")?.let { File(it) }?.json()
        ?: Bootstrap::class.java.getResource("/config.json")?.readText()
            ?.json<Config>()?.config?.fromUrl() ?: error("No config found")
    Bootstrap.bootstrap(config, args + config.arguments)
}