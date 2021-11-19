package dev.koding.launcher.data.runtime

import dev.koding.launcher.data.manifest.LaunchJavaVersion
import dev.koding.launcher.util.OS

fun JavaRuntime.select() = when {
    OS.type == OS.Type.WINDOWS && OS.arch == "x86" -> windows32
    OS.type == OS.Type.WINDOWS && OS.arch == "x64" -> windows64
    OS.type == OS.Type.MAC -> macOs
    OS.type == OS.Type.LINUX -> linux
    else -> null
}

fun List<JavaRuntimeData>.match(versionData: LaunchJavaVersion) =
    filter {
        val version = it.version.name.split("_").first().split(".")
        if (versionData.majorVersion != version[0].toIntOrNull()) return@filter false
        true
    }.firstOrNull()