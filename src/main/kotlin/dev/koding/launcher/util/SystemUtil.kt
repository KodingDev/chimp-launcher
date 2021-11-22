package dev.koding.launcher.util

import java.io.File
import java.security.MessageDigest
import javax.swing.JOptionPane


val File.sha1: String
    get() {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(readBytes())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

object OS {
    enum class Type {
        WINDOWS,
        LINUX,
        MAC,
        UNKNOWN
    }

    val type: Type
        get() {
            val osName = System.getProperty("os.name")
            return when {
                osName.contains("Windows", true) -> Type.WINDOWS
                osName.contains("Mac", true) -> Type.MAC
                osName.contains("Linux", true) -> Type.LINUX
                else -> Type.UNKNOWN
            }
        }

    val name = when {
        System.getProperty("os.name").startsWith("Windows") -> "windows"
        System.getProperty("os.name").startsWith("Mac") -> "osx"
        System.getProperty("os.name").startsWith("Linux") -> "linux"
        else -> "unknown"
    }

    val version: String = System.getProperty("os.version")
    val arch: String = System.getProperty("os.arch")
}

object InputUtil {
    fun showError(title: String, message: String) =
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE)

    fun askSelection(prompt: String, vararg options: String) =
        JOptionPane.showOptionDialog(
            null,
            prompt,
            "Chimp Launcher",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        )
}
