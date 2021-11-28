package dev.koding.launcher.loader

import dev.koding.launcher.launch.ResourcesDirectory
import dev.koding.launcher.util.download
import dev.koding.launcher.util.json
import dev.koding.launcher.util.system.sha1
import dev.koding.launcher.util.system.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.io.File
import java.net.URL

@Serializable
sealed class Resource {
    abstract val name: String
    val location get() = ResourceLocation(name.substringBefore(":"), name.substringAfter(":").split("/"))
}

@Serializable
@SerialName("named")
data class NamedResource(
    override val name: String,
) : Resource() {
    companion object : ResourceLoader<NamedResource> {
        override suspend fun load(manager: ResourceManager, resource: NamedResource): LoadedResource<*>? =
            manager.resolvers.firstNotNullOfOrNull { it.resolve(manager, resource.location) }
    }
}

@Serializable
@SerialName("url")
data class UrlResource(
    override val name: String,
    val url: String,
    val integrity: Integrity? = null,
    val path: String? = null,
    val volatile: Boolean = false
) : Resource() {
    companion object : ResourceLoader<UrlResource> {
        private val logger = KotlinLogging.logger {}

        override suspend fun load(manager: ResourceManager, resource: UrlResource): LoadedResource<*> {
            val parsed = withContext(Dispatchers.Default) { URL(resource.url) }
            val target = manager.config[ResourcesDirectory]?.resolve(resource.path ?: "${parsed.host}/${parsed.path}")
                ?: error("No resources directory specified")

            val loaded = LoadedResource(resource, target)
            if (target.exists() && !resource.volatile && resource.integrity?.verify(target) != false) return loaded

            logger.info { "Downloading ${resource.name} to ${target.absolutePath}" }
            parsed.download(target)
            logger.debug { "Downloaded ${resource.name} with SHA1 ${target.sha1}" }

            if (resource.integrity?.verify(target) != false) return loaded
            error("Integrity check failed for ${resource.name}")
        }
    }

    @Serializable
    data class Integrity(
        val sha1: String? = null,
        val sha256: String? = null
    ) {
        fun verify(file: File) =
            sha1?.let { file.sha1 == it } ?: true && sha256?.let { file.sha256 == it } ?: true
    }
}

@Serializable
@SerialName("file")
data class FileResource(
    override val name: String,
    val path: String
) : Resource() {
    companion object : ResourceLoader<FileResource> {
        override suspend fun load(manager: ResourceManager, resource: FileResource) =
            File(resource.path).takeIf { it.exists() }?.let { LoadedResource(resource, it) }
    }
}

data class LoadedResource<T : Resource>(
    val resource: T,
    val file: File
) {
    inline fun <reified T> json() = file.json<T>()
}