package dev.koding.launcher.util

import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.security.MessageDigest
import javax.swing.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


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

    suspend fun askSelection(prompt: String, vararg options: String) =
        suspendCoroutine<String?> { cont ->
            JFrame("Chimp Launcher").apply frame@{
                setSize(300, 120)
                setLocationRelativeTo(null)

                contentPane = JPanel().apply {
                    val selection = JComboBox(options).apply { alignmentX = Component.CENTER_ALIGNMENT }

                    border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)

                    add(JLabel(prompt).apply { alignmentX = Component.CENTER_ALIGNMENT })
                    add(Box.createVerticalStrut(5))
                    add(selection)
                    add(Box.createVerticalStrut(5))

                    add(JPanel().apply {
                        alignmentX = Component.CENTER_ALIGNMENT
                        layout = BorderLayout()

                        add(JButton("Select").apply {
                            addActionListener {
                                cont.resume(selection.selectedItem?.toString() ?: return@addActionListener)
                                this@frame.isVisible = false
                            }
                        }, BorderLayout.CENTER)
                    })
                }

                addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent?) {
                        cont.resume(null)
                    }
                })
                isVisible = true
            }
        }
}
