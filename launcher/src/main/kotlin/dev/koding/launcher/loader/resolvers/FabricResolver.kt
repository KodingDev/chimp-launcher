package dev.koding.launcher.loader.resolvers

import dev.koding.launcher.data.launcher.Download
import dev.koding.launcher.loader.*
import io.ktor.http.*
import io.ktor.util.*

object FabricResolver : ResourceResolver {
    override suspend fun resolve(manager: ResourceManager, resource: ResourceLocation): LoadedResource<*>? {
        if (!resource.namespace.equals("fabric", true)) return null
        if (resource.path.size < 2) return null

        val (intermediary, loader) = resource.path
        val url = url {
            takeFrom("https://fabricmc.net/download/vanilla/")
            parameters.apply {
                this["format"] = "profileJson"
                this["intermediary"] = intermediary
                this["loader"] = loader
            }
        }

        return manager.load(
            UrlResource(
                resource.toString(),
                Download(url, path = "fabric/$intermediary/$loader/version.json")
            )
        )
    }
}