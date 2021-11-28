package dev.koding.launcher.util.system

import java.io.File
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

fun File.extractZip(destination: File) {
    if (!exists()) return
    val zipFile = ZipFile(this)
    val entries = zipFile.entries()

    while (entries.hasMoreElements()) {
        val entry = entries.nextElement()
        val entryName = entry.name
        if (entryName.endsWith("/")) {
            destination.resolve(entryName).mkdirs()
        } else {
            val inputStream = zipFile.getInputStream(entry)
            val outputStream = destination.resolve(entryName).outputStream()
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
        }
    }
}