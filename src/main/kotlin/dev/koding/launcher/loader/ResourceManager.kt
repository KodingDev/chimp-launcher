package dev.koding.launcher.loader

import dev.koding.launcher.Launcher
import dev.koding.launcher.data.minecraft.manifest.LauncherManifest
import dev.koding.launcher.util.json
import dev.koding.launcher.util.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.net.URL

// TODO: Option to not cache
// TODO: Clean this up (ew)
class ResourceManager {

    companion object {
        operator fun invoke(block: ResourceManager.() -> Unit) = ResourceManager().apply(block)
    }

    private val root = Launcher.home.resolve("resources")
    private val logger = KotlinLogging.logger {}
    private val resources = mutableMapOf<String, LoadedResource>()
    private val resolvers = mutableListOf<ResourceResolver>()

    suspend fun load(resource: Resource): LoadedResource? {
        val res = this[resource.name]
        if (res != null) return res

        val loaded = when (resource) {
            is FileResource -> loadFile(resource)
            is UrlResource -> loadUrl(resource)
        }
        resources[resource.name] = loaded ?: return null
        return loaded
    }

    suspend inline fun <reified T> loadAs(resource: Resource): T? =
        load(resource)?.file?.json<T>()

    private fun loadFile(data: FileResource) =
        File(data.path).takeIf { it.exists() }
            ?.let { LoadedResource(data.name.toLocation(), it) }

    private suspend fun loadUrl(data: UrlResource): LoadedResource {
        val parsed = withContext(Dispatchers.Default) { URL(data.url) }
        val target = root.resolve("${parsed.host}/${parsed.path}")
        val loadedResource = LoadedResource(data.name.toLocation(), target).also { resources[data.name] = it }

        val manifest = runCatching { loadedResource.getManifest() }.getOrNull()
        if (target.exists() && manifest == data) return loadedResource

        logger.info { "Downloading resource (${loadedResource.name}): ${data.url} -> ${target.absolutePath}" }
        target.parentFile.mkdirs()
        withContext(Dispatchers.IO) {
            parsed.openStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }

        loadedResource.saveManifest(data)
        return loadedResource
    }

    private fun LoadedResource.saveManifest(resource: Resource) =
        file.parentFile.resolve("${file.name}.manifest").writeText(resource.toJson())

    private fun LoadedResource.getManifest(): Resource? {
        val manifest = file.parentFile.resolve("${file.name}.manifest")
        return manifest.takeIf { it.exists() }?.json()
    }

    operator fun get(name: String) = resources[name]

    suspend fun getOrResolve(name: String) =
        this[name] ?: resources[name] ?: resolvers.firstNotNullOfOrNull { it.resolve(this, name.toLocation()) }

    suspend fun getManifest(name: String) = LauncherManifest.load(this, name)

    operator fun ResourceResolver.unaryPlus() = resolvers.add(this)

    data class LoadedResource(
        val name: ResourceLocation,
        val file: File
    )
}

interface ResourceResolver {
    suspend fun resolve(manager: ResourceManager, path: ResourceLocation): ResourceManager.LoadedResource?
}

data class ResourceLocation(
    val namespace: String,
    val path: String
) {
    override fun toString() = "$namespace:$path"
}

fun String.toLocation() = ResourceLocation(substringBefore(":"), substringAfter(":"))