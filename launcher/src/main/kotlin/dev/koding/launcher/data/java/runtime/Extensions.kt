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

package dev.koding.launcher.data.java.runtime

import dev.koding.launcher.data.minecraft.manifest.LauncherManifest
import dev.koding.launcher.util.system.OS

fun JavaRuntime.select() = when {
    OS.type == OS.Type.WINDOWS && OS.arch == "x86" -> windows32
    OS.type == OS.Type.WINDOWS && OS.arch == "amd64" -> windows64
    OS.type == OS.Type.MAC -> macOs
    OS.type == OS.Type.LINUX -> linux
    else -> null
}

fun List<JavaRuntime.Data>.match(versionData: LauncherManifest.JavaVersion) =
    filter {
        val rawVersion = it.version.name.split("_").first()
        val version = if ("." in rawVersion) rawVersion.split(".")
        else rawVersion.split("u")

        if (versionData.majorVersion != version[0].toIntOrNull()) return@filter false
        true
    }.firstOrNull()