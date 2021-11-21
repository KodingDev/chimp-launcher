package dev.koding.launcher.auth

import dev.koding.launcher.util.InputUtil
import javax.swing.JOptionPane

class MojangAuthProvider : AuthProvider() {
    override suspend fun login(current: AuthData?): AuthData {
        if (current?.token?.accessToken != null) {
            if (MinecraftAPI.validate(current.token.accessToken)) return current
            val refreshed = MinecraftAPI.refresh(current.token.accessToken)
            return MojangAuthData(refreshed.selectedProfile, MinecraftToken(refreshed.accessToken, 0L))
        }

        // Prompt for username and password
        val username = JOptionPane.showInputDialog("Enter your username:") ?: error("Cancelled")
        val password = InputUtil.askPassword("Enter your password:") ?: error("Cancelled")

        // Return the auth data
        val auth = MinecraftAPI.login(username, password)
        return MojangAuthData(auth.selectedProfile, MinecraftToken(auth.accessToken, 0L))
    }
}