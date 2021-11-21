package dev.koding.launcher.util

import java.io.File
import java.security.MessageDigest
import javax.swing.Box
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPasswordField


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
    fun askPassword(prompt: String): String? {
        val jpf = JPasswordField()
        val box = Box.createVerticalBox().apply {
            add(JLabel(prompt))
            add(jpf)
        }

        val dialog = JOptionPane.showConfirmDialog(null, box, "Input", JOptionPane.OK_CANCEL_OPTION)
        return if (dialog == JOptionPane.OK_OPTION) jpf.password.concatToString() else null
    }

    fun askSelection(prompt: String, vararg options: String) =
        JOptionPane.showOptionDialog(
            null,
            prompt,
            "Input",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        )
}
