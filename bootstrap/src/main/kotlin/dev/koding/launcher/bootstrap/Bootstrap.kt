package dev.koding.launcher.bootstrap

import dev.koding.launcher.data.launcher.download
import dev.koding.launcher.launcherHome
import java.io.File
import java.net.URLClassLoader
import javax.swing.UIManager


object Bootstrap {
    fun bootstrap(manifest: Manifest, args: Array<String>) {
        val libDir = launcherHome.resolve("launcher/libraries")
        val dependencies = manifest.dependencies.map { it.toDownload() }.map { it.download(libDir).toURI().toURL() }

        val classLoader = URLClassLoader(dependencies.toTypedArray(), null)
        UIManager.put("ClassLoader", classLoader)

        val launcher = classLoader.loadClass(manifest.main)
        launcher.getMethod("main", Array<String>::class.java).invoke(null, args)
    }
}

fun main(args: Array<String>) {
    val config: Manifest = System.getProperty("launcher.config")?.let { File(it) }?.json()
        ?: Bootstrap::class.java.getResource("/config.json")?.readText()
            ?.json<Config>()?.config?.fromUrl() ?: error("No config found")
    Bootstrap.bootstrap(config, args + config.arguments)
}