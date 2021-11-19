package dev.koding.launcher.util

import java.io.File
import java.security.MessageDigest

val File.sha1: String
    get() {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(readBytes())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

object OS {
    val name = when {
        System.getProperty("os.name").startsWith("Windows") -> "windows"
        System.getProperty("os.name").startsWith("Mac") -> "osx"
        System.getProperty("os.name").startsWith("Linux") -> "linux"
        else -> "unknown"
    }

    val version: String = System.getProperty("os.version")
    val arch: String = System.getProperty("os.arch")
}