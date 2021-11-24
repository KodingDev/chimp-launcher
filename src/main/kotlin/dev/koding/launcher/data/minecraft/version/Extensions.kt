package dev.koding.launcher.data.minecraft.version

operator fun VersionManifest.get(key: String) = versions.firstOrNull { it.id == key }