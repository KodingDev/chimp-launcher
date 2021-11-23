package dev.koding.launcher.util.system

import dev.koding.launcher.util.ui.alignX
import dev.koding.launcher.util.ui.content
import dev.koding.launcher.util.ui.frame
import dev.koding.launcher.util.ui.sized
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JOptionPane
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object SwingUtil {
    fun showError(title: String, message: String) =
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE)

    suspend fun askSelection(prompt: String, vararg options: String) =
        suspendCoroutine<String?> { cont ->
            frame {
                content {
                    padding = 10

                    val selection = JComboBox(options).alignX(Component.CENTER_ALIGNMENT).sized(300, 25)
                    +JLabel(prompt).alignX(Component.CENTER_ALIGNMENT).sized(300, 25)
                    +verticalSpace(5)
                    +selection
                    +verticalSpace(5)

                    +panel {
                        panel.alignmentX = Component.CENTER_ALIGNMENT
                        layout = BorderLayout()

                        button("Select") {
                            cont.resume(selection.selectedItem?.toString() ?: return@button)
                            this@frame.dispose()
                        }.apply { preferredSize = Dimension(300, 25) } + BorderLayout.CENTER
                    }

                    addWindowListener(object : WindowAdapter() {
                        override fun windowClosing(e: WindowEvent?) {
                            cont.resume(null)
                        }
                    })
                }

                pack()
            }.requestFocus()
        }
}
