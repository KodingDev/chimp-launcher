package dev.koding.launcher.util

import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipFile

val File.sha1: String
    get() {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(readBytes())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

val File.sha256: String
    get() {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(readBytes())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

fun URL.download(file: File) {
    file.parentFile.mkdirs()
    openConnection().apply {
        setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36"
        )
    }.getInputStream().use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
}

fun File.extractZip(destination: File, eliminateRoot: Boolean = false) {
    destination.mkdirs()
    ZipFile(this).use { zip ->
        val root = zip.entries().nextElement().name.substringBefore('/')
        val commonRoot = zip.entries().asSequence().all { it.name.startsWith(root) }

        zip.entries().asSequence().forEach { entry ->
            val name = if (commonRoot && eliminateRoot) entry.name.substringAfter(root) else entry.name
            val file = File(destination, name)

            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile.mkdirs()
                zip.getInputStream(entry).use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
}