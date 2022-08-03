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

import dev.koding.launcher.util.json
import dev.koding.launcher.util.ktor
import io.ktor.http.*
import java.io.File
import java.net.URI
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

data class LoadedResource(
    val resource: URI,
    val file: File
) {
    inline fun <reified T> json() = file.json<T>()
}

data class ResourceDescriptor(var resource: URI) {
    var path by ParamAccessor(this::resource, "path") { it ?: "${resource.host}/${resource.path}" }

    var sha1 by ParamAccessor(this::resource, "sha1") { it }
    var sha256 by ParamAccessor(this::resource, "sha256") { it }

    var volatile by ParamAccessor(this::resource, "volatile") { it?.toBoolean() ?: false }
    var log by ParamAccessor(this::resource, "log") { it?.toBoolean() ?: true }

    private class ParamAccessor<T>(var property: KMutableProperty<URI>, name: String, val modify: (String?) -> T) {
        private val name = "chimp.$name"

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
            modify(this.property.call().ktor.parameters[name])

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            this.property.setter.call(URLBuilder(this.property.call().ktor).apply {
                if (value == null) parameters.remove(name)
                else parameters[name] = value.toString()
            }.build().toURI())
        }
    }
}

fun URI.describe(block: ResourceDescriptor.() -> Unit) = ResourceDescriptor(this).apply(block).resource

@Suppress("unused")
fun buildUrl(block: URLBuilder.() -> Unit): URI = URLBuilder().apply(block).build().toURI()
