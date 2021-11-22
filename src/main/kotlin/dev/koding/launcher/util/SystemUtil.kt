package dev.koding.launcher.util

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import javax.swing.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


val File.sha1: String
    get() {
        val md = MessageDigest.getInstance("SHA-1")
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

object OS {
    enum class Type(val names: Array<String>) {
        WINDOWS(arrayOf("windows")),
        LINUX(arrayOf("linux")),
        MAC(arrayOf("osx", "macos")),
        UNKNOWN(emptyArray())
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
                contentPane = JPanel().apply {
                    val selection = JComboBox(options).apply {
                        alignmentX = Component.CENTER_ALIGNMENT
                        preferredSize = Dimension(300, 25)
                    }

                    border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)

                    add(JLabel(prompt).apply {
                        alignmentX = Component.CENTER_ALIGNMENT
                        preferredSize = Dimension(300, 25)
                    })
                    add(Box.createVerticalStrut(5))
                    add(selection)
                    add(Box.createVerticalStrut(5))

                    add(JPanel().apply {
                        alignmentX = Component.CENTER_ALIGNMENT
                        layout = BorderLayout()

                        add(JButton("Select").apply {
                            preferredSize = Dimension(300, 25)
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

                pack()
                setLocationRelativeTo(null)
                isVisible = true
            }
        }
}
