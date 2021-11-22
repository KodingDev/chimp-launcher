package dev.koding.launcher.util.system

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object SwingUtil {
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
                requestFocus()
            }
        }
}
