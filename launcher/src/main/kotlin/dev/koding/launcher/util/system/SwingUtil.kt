/*
 *    Copyright 2022 Koding Dev
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package dev.koding.launcher.util.system

import dev.koding.launcher.util.ui.alignX
import dev.koding.launcher.util.ui.content
import dev.koding.launcher.util.ui.dialog
import dev.koding.launcher.util.ui.sized
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Desktop
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.URI
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
            dialog {
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
                            this@dialog.dispose()
                        }.apply { preferredSize = Dimension(300, 25) } + BorderLayout.CENTER
                    }

                    addWindowListener(object : WindowAdapter() {
                        override fun windowClosing(e: WindowEvent?) {
                            cont.resume(null)
                        }
                    })
                }

                requestFocusInWindow()
                pack()
            }
        }

    fun browse(url: String) {
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.BROWSE)) return desktop.browse(URI(url))
        Runtime.getRuntime().exec("xdg-open $url")
    }
}
