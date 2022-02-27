package dev.koding.launcher.loader.resolvers

import dev.koding.launcher.loader.LoadedResource
import dev.koding.launcher.loader.ResourceManager
import dev.koding.launcher.loader.ResourceResolver
import dev.koding.launcher.loader.describe
import dev.koding.launcher.util.pathComponents
import java.net.URI

object FabricResolver : ResourceResolver {

    override fun matches(uri: URI) = uri.scheme.equals("content", true) &&
            uri.host.equals("net.fabricmc", true) &&
            uri.path.split("/").size >= 2

    override suspend fun resolve(manager: ResourceManager, resource: URI): LoadedResource? {
        val (intermediary, loader) = resource.pathComponents
        val url = URI("https://meta.fabricmc.net/v2/versions/loader/$intermediary/$loader/profile/json")
            .describe { path = "fabric/$intermediary/$loader/version.json" }
        return manager.load(url)
    }
}