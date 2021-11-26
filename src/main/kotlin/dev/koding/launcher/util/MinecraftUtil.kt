package dev.koding.launcher.util

import dev.koding.launcher.util.system.OS
import java.io.File

val minecraftHome = when (OS.type) {
    OS.Type.WINDOWS -> System.getenv("APPDATA") + "\\.minecraft"
    OS.Type.MAC -> System.getProperty("user.home") + "/Library/Application Support/minecraft"
    OS.Type.LINUX -> System.getProperty("user.home") + "/.minecraft"
    else -> error("Unsupported device")
}.let { File(it) }