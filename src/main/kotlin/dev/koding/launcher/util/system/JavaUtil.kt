package dev.koding.launcher.util.system

import java.io.File

object JavaUtil {

    val javaExecutable = getJavaPath(File(System.getProperty("java.home")))

    fun getJavaPath(root: File) = when (OS.type) {
        OS.Type.WINDOWS -> root.resolve("bin/java.exe")
        OS.Type.MAC -> root.resolve("jre.bundle")
            .let { if (it.exists()) it.resolve("Contents/Home") else root }
            .resolve("bin/java")
        OS.Type.LINUX -> root.resolve("bin/java")
        else -> throw IllegalStateException("Unsupported OS: ${OS.type}")
    }

}