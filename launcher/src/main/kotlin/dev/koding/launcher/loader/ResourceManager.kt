package dev.koding.launcher.loader

import dev.koding.launcher.launch.Config
import dev.koding.launcher.loader.resolvers.DefaultResourceResolver
import java.net.URI

class ResourceManager {

    companion object {
        // TODO: Remove this
        operator fun invoke(block: ResourceManager.() -> Unit): ResourceManager {
            val resourceManager = ResourceManager()
            resourceManager.block()
            return resourceManager
        }
    }

    private val resources = mutableMapOf<URI, LoadedResource>()
    private val resolvers = arrayListOf<ResourceResolver>()

    val config = Config()

    suspend fun load(resource: URI) = (resolvers.find { it.matches(resource) } ?: DefaultResourceResolver)
        .resolve(this, resource)
        ?.also { resources[resource] = it }

    operator fun get(uri: URI) = resources[uri]
    operator fun ResourceResolver.unaryPlus() = resolvers.add(this)

}

interface ResourceResolver {
    fun matches(uri: URI): Boolean

    suspend fun resolve(manager: ResourceManager, resource: URI): LoadedResource?
}