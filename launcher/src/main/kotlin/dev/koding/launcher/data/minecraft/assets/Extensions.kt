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

package dev.koding.launcher.data.minecraft.assets

import dev.koding.launcher.data.minecraft.manifest.Asset
import java.net.URI

fun Map.Entry<String, AssetIndex.Object>.toAsset() = "${value.hash.substring(0..1)}/${value.hash}"
    .let {
        Asset(
            URI("https://resources.download.minecraft.net/$it"),
            value.size,
            sha1 = value.hash,
            path = it
        )
    }