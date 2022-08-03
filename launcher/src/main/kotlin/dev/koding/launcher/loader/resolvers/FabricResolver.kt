/*
 *    Copyright 2022 Koding Dev
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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