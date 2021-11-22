@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.auth

import dev.koding.launcher.util.InputUtil
import dev.koding.launcher.util.json
import io.ktor.client.features.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import mu.KotlinLogging
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.system.exitProcess

class MojangAuthProvider : AuthProvider() {
    private val logger = KotlinLogging.logger {}

    override suspend fun login(current: AuthData?): AuthData {
        if (current?.token?.accessToken != null) {
            logger.debug { "Already logged in" }
            if (MinecraftAPI.validate(current.token.accessToken)) {
                logger.debug { "Existing token is valid" }
                return current
            }

            logger.debug { "Refreshing token" }
            val refreshed = MinecraftAPI.refresh(current.token.accessToken)
            return MojangAuthData(refreshed.selectedProfile, MinecraftToken(refreshed.accessToken, 0L))
        }

        // Show username and password frame
        val username = JTextField("")
        val password = JPasswordField("")

        suspendCoroutine<Boolean> { cont ->
            JFrame("Mojang Authentication").apply frame@{
                setSize(400, 200)
                setLocationRelativeTo(null)

                contentPane = JPanel().apply {
                    border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)

                    add(JLabel("Username", SwingConstants.LEFT).apply { alignmentX = Component.LEFT_ALIGNMENT })
                    add(Box.createVerticalStrut(5))
                    add(username.apply { alignmentX = Component.LEFT_ALIGNMENT })
                    add(Box.createVerticalStrut(10))

                    add(JLabel("Password").apply { alignmentX = Component.LEFT_ALIGNMENT })
                    add(Box.createVerticalStrut(5))
                    add(password.apply { alignmentX = Component.LEFT_ALIGNMENT })
                    add(Box.createVerticalStrut(10))

                    add(JPanel().apply {
                        alignmentX = Component.LEFT_ALIGNMENT
                        layout = BorderLayout()

                        add(JButton("Login").apply {
                            addActionListener {
                                cont.resume(true)
                                this@frame.isVisible = false
                            }
                        }, BorderLayout.CENTER)
                    })
                }

                isVisible = true
            }
        }

        // Return the auth data
        logger.info { "Logging in with Mojang" }
        val auth = try {
            MinecraftAPI.login(username.text, password.password.concatToString())
        } catch (e: Exception) {
            if (e is ClientRequestException) {
                val error = json.decodeFromStream<MojangAuthError>(e.response.content.toInputStream())
                InputUtil.showError("Error logging in", "${error.error}: ${error.errorMessage}")
            }

            logger.error(e) { "Failed to login with Mojang" }
            exitProcess(0)
        }

        return MojangAuthData(auth.selectedProfile, MinecraftToken(auth.accessToken, 0L))
    }

    @Serializable
    data class MojangAuthError(
        val error: String,
        val errorMessage: String
    ) : Exception(errorMessage)
}