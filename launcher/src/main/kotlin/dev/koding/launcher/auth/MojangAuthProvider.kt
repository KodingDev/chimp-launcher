@file:OptIn(ExperimentalSerializationApi::class)

package dev.koding.launcher.auth

import dev.koding.launcher.util.json
import dev.koding.launcher.util.system.SwingUtil
import dev.koding.launcher.util.ui.alignX
import dev.koding.launcher.util.ui.content
import dev.koding.launcher.util.ui.dialog
import io.ktor.client.features.*
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JPasswordField
import javax.swing.JTextField
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
            dialog(size = 400 to 200) {
                content {
                    padding = 10

                    +JLabel("Username").alignX(Component.LEFT_ALIGNMENT)
                    +verticalSpace(5)
                    +username.alignX(Component.LEFT_ALIGNMENT)
                    +verticalSpace(10)

                    +JLabel("Password").alignX(Component.LEFT_ALIGNMENT)
                    +verticalSpace(5)
                    +password.alignX(Component.LEFT_ALIGNMENT)
                    +verticalSpace(10)

                    +panel {
                        layout = BorderLayout()

                        button("Login") {
                            cont.resume(true)
                            this@dialog.dispose()
                        } + BorderLayout.CENTER
                    }.alignX(Component.LEFT_ALIGNMENT)
                }
            }
        }

        // Return the auth data
        logger.info { "Logging in with Mojang" }
        val auth = try {
            MinecraftAPI.login(username.text, password.password.concatToString())
        } catch (e: Exception) {
            if (e is ClientRequestException) {
                val error = e.response.content.toByteArray().decodeToString().json<MojangAuthError>()
                SwingUtil.showError("Error logging in", "${error.error}: ${error.errorMessage}")
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