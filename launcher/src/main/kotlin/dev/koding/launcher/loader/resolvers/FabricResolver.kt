package dev.koding.launcher.loader.resolvers

import dev.koding.launcher.loader.*
import dev.koding.launcher.util.pathComponents
import io.ktor.http.*
import java.net.URI

object FabricResolver : ResourceResolver {

    override fun matches(uri: URI) = uri.scheme.equals("content", true) &&
            uri.host.equals("net.fabricmc", true) &&
            uri.path.split("/").size >= 2

    override suspend fun resolve(manager: ResourceManager, resource: URI): LoadedResource? {
        val (intermediary, loader) = resource.pathComponents
        val url = buildUrl {
            takeFrom("https://fabricmc.net/download/vanilla/")
            parameters.apply {
                this["format"] = "profileJson"
                this["intermediary"] = intermediary
                this["loader"] = loader
            }
        }.describe { path = "fabric/$intermediary/$loader/version.json" }
        return manager.load(url)
    }
}