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

package dev.koding.launcher.data.minecraft.version

import dev.koding.launcher.util.fromUrl
import kotlinx.serialization.Serializable

@Serializable
data class VersionManifest(
    val versions: List<Version>
) {
    companion object {
        fun fetch() = "https://launchermeta.mojang.com/mc/game/version_manifest.json".fromUrl<VersionManifest>()
    }

    @Serializable
    data class Version(
        val id: String,
        val url: String
    )
}