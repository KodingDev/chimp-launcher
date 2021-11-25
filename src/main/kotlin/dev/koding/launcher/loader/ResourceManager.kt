package dev.koding.launcher.loader

import dev.koding.launcher.launch.Config

class ResourceManager {

    companion object {
        operator fun invoke(block: ResourceManager.() -> Unit): ResourceManager {
            val resourceManager = ResourceManager()
            resourceManager.block()
            return resourceManager
        }
    }

    private val resources = mutableMapOf<String, LoadedResource<*>>()
    private val loaders = hashMapOf<Class<*>, ResourceLoader<*>>()

    val resolvers = arrayListOf<ResourceResolver>()
    val config = Config()

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Resource> load(resource: T): LoadedResource<*>? {
        val res = this[resource.name]
        if (res != null) return res as? LoadedResource<T>

        return (loaders[resource::class.java] as? ResourceLoader<T>)
            ?.load(this, resource)
            ?.also { resources[resource.name] = it }
    }

    operator fun get(name: String) = resources[name]
    operator fun ResourceResolver.unaryPlus() = resolvers.add(this)

    operator fun <T : Resource> ResourceLoader<T>.unaryPlus() {
        loaders[this::class.java.declaringClass] = this
    }
}

data class ResourceLocation(
    val namespace: String,
    val path: String
) {
    override fun toString() = "$namespace:$path"
}

interface ResourceResolver {
    suspend fun resolve(manager: ResourceManager, path: ResourceLocation): LoadedResource<*>?
}

interface ResourceLoader<T : Resource> {
    suspend fun load(manager: ResourceManager, resource: T): LoadedResource<*>?
}