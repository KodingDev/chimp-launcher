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

@file:OptIn(ObsoleteCoroutinesApi::class)

package dev.koding.launcher.data.minecraft.manifest

import dev.koding.launcher.data.launcher.Download
import dev.koding.launcher.util.system.OS
import kotlinx.coroutines.ObsoleteCoroutinesApi
import java.net.URI

fun Asset.asDownload(): Download? {
    return Download(
        url = url ?: return null,
        path = path ?: url.path.split("/").last(),
        integrity = Download.Integrity(size = size, sha1 = sha1)
    )
}

fun Rule.matches(): Boolean {
    if (os != null) {
        if (os.name != null && OS.type.names.none { it == os.name }) return false
        if (os.version != null && OS.version.matches(os.version.toRegex())) return false
        if (os.arch != null && os.arch != OS.arch) return false
    }
    if (features != null) return false // future
    return true
}

fun List<Rule>.matches(default: Rule.Action): Rule.Action {
    if (isEmpty()) return Rule.Action.ALLOW
    var action = default
    forEach { if (it.matches()) action = it.action }
    return action
}

fun List<LauncherManifest.Arguments.Argument>.filterMatchesRule(default: Rule.Action) =
    filter { it.rules.matches(default) == Rule.Action.ALLOW }

fun List<LauncherManifest.Arguments.Argument>.toFilteredArray() =
    filterMatchesRule(Rule.Action.DISALLOW).map { it.value }.toTypedArray()

operator fun LauncherManifest.Arguments.plus(other: LauncherManifest.Arguments) =
    LauncherManifest.Arguments(
        game = game.plus(other.game),
        jvm = jvm.plus(other.jvm)
    )

fun List<LauncherManifest.Library>.filterMatchesRule() =
    filter { it.rules.matches(Rule.Action.DISALLOW) == Rule.Action.ALLOW }

val LauncherManifest.Library.asset: Asset
    get() {
        downloads?.artifact?.let { return it }
        val path = name.split(":").let { "${it[0].replace(".", "/")}/${it[1]}/${it[2]}/${it[1]}-${it[2]}.jar" }
        return Asset(url = if (url != null) URI("$url$path") else null, path = path)
    }

val LauncherManifest.Library.assets
    get() = listOfNotNull(
        asset,
        *(downloads?.classifiers?.filterKeys { it !in OS.classifiers }?.values?.toTypedArray() ?: emptyArray())
    )

val LauncherManifest.Library.nativeAsset
    get() = OS.type.names.firstNotNullOfOrNull { natives[it] }?.let { downloads?.classifiers?.get(it) }

fun LauncherManifest.Library.isNative(asset: Asset) =
    downloads?.classifiers?.entries?.firstOrNull { it.value == asset }?.key?.let { it in natives.values } ?: false

fun List<String>.toArguments() = map { LauncherManifest.Arguments.Argument(it) }