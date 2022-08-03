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

package dev.koding.launcher.data.launcher

import dev.koding.launcher.util.URISerializer
import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
data class RemoteConfig(
    val profiles: List<RemoteProfile>
)

@Serializable
data class RemoteProfile(
    val name: String,
    @Serializable(URISerializer::class)
    val resource: URI,
    val description: String? = null
)