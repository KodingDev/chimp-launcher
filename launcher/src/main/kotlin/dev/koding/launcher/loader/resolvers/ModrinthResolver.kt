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

import dev.koding.launcher.data.modrinth.ModrinthAPI
import dev.koding.launcher.launch.ResourcesDirectory
import dev.koding.launcher.loader.LoadedResource
import dev.koding.launcher.loader.ResourceManager
import dev.koding.launcher.loader.ResourceResolver
import dev.koding.launcher.loader.describe
import dev.koding.launcher.util.pathComponents
import mu.KotlinLogging
import java.net.URI

object ModrinthResolver : ResourceResolver {
    private val logger = KotlinLogging.logger {}

    override fun matches(uri: URI) = uri.scheme.equals("content", true) &&
            uri.host.equals("com.modrinth", true)

    override suspend fun resolve(manager: ResourceManager, resource: URI): LoadedResource? {
        val mod = resource.pathComponents.first()
        val versionName = resource.path.getOrNull(1) ?: "latest"

        // For speed's sake we will assume that the file is the correct mod
        // if it already exists
        val target = "modrinth/${mod}/${versionName}/${mod}-${versionName}.jar"
        val targetFile = manager.config[ResourcesDirectory]?.resolve(target)
            ?: error("No resources directory configured")
        if (targetFile.exists()) return manager.load(targetFile.toURI())

        logger.debug { "Searching for modrinth resource $resource" }
        val result = ModrinthAPI.search(mod)
        if (result.hits.isEmpty()) return null

        logger.debug { "Fetching versions for modrinth resource $resource" }
        val hit = result.hits.sortedBy {
            if (it.slug.equals(mod, true)) return@sortedBy 0
            if (it.slug.replace("-", "").equals(mod, true)) return@sortedBy 5
            if (it.modId.equals(mod, true)) return@sortedBy 5
            10
        }.firstOrNull() ?: return null

        val versions = ModrinthAPI.getVersions(hit.modId)
        if (versions.isEmpty()) return null

        val version = (if (resource.pathComponents.size == 1) versions.first()
        else versions.find { it.versionNumber == resource.pathComponents[1] }) ?: return null

        val file = version.files.firstOrNull() ?: return run {
            logger.debug { "No version found for modrinth resource $resource" }
            null
        }

        return manager.load(
            URI(file.url).describe {
                path = target
                sha1 = file.hashes.sha1
                sha256 = file.hashes.sha256
            }
        )
    }
}