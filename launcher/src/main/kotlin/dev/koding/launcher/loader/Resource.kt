package dev.koding.launcher.loader

import dev.koding.launcher.data.launcher.Download
import dev.koding.launcher.data.launcher.download
import dev.koding.launcher.launch.ResourcesDirectory
import dev.koding.launcher.util.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

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
    val download: Download
) : Resource() {
    companion object : ResourceLoader<UrlResource> {
        override suspend fun load(manager: ResourceManager, resource: UrlResource): LoadedResource<*> {
            val target = manager.config[ResourcesDirectory] ?: error("No resources directory specified")
            return LoadedResource(resource, resource.download.download(target))
        }
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