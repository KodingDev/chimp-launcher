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