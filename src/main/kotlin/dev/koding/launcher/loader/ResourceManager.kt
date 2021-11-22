package dev.koding.launcher.loader

import dev.koding.launcher.Launcher
import dev.koding.launcher.data.config.ProfileResource
import dev.koding.launcher.data.config.UrlResource
import dev.koding.launcher.data.manifest.LauncherManifest
import dev.koding.launcher.util.json
import dev.koding.launcher.util.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.net.URL

class ResourceManager {

    private val root = Launcher.home.resolve("resources")
    private val logger = KotlinLogging.logger {}
    private val resources = mutableMapOf<String, Resource>()

    suspend fun load(resource: ProfileResource) = when (resource) {
        is UrlResource -> loadUrl(resource)
    }

    private suspend fun loadUrl(data: UrlResource) {
        val parsed = withContext(Dispatchers.Default) { URL(data.url) }
        val target = root.resolve("${parsed.host}/${parsed.path}")
        val resource = Resource(data.name, target).also { resources[data.name] = it }

        val manifest = resource.getManifest()
        if (target.exists() && manifest == data) return

        logger.info { "Downloading resource (${resource.name}): ${data.url} -> ${target.absolutePath}" }
        target.parentFile.mkdirs()
        withContext(Dispatchers.IO) {
            parsed.openStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }

        resource.saveManifest(data)
    }

    private fun Resource.saveManifest(resource: ProfileResource) =
        file.parentFile.resolve("${file.name}.manifest").writeText(resource.toJson())

    private fun Resource.getManifest(): ProfileResource? {
        val manifest = file.parentFile.resolve("${file.name}.manifest")
        return manifest.takeIf { it.exists() }?.json()
    }

    operator fun get(name: String) = resources[name]
    fun getManifest(name: String) = LauncherManifest.load(this, name)

}

data class Resource(
    val name: String,
    val file: File
)