package dev.koding.launcher.auth

import dev.koding.launcher.util.InputUtil
import dev.koding.launcher.util.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
sealed class AuthData {
    companion object {
        fun fromJson(text: String) = json.decodeFromString<AuthData>(text)
    }

    abstract val profile: MinecraftAPI.MinecraftProfile
    abstract val token: MinecraftToken

    fun toJson() = json.encodeToString(this)
}

@Serializable
data class MicrosoftAuthData(
    override val profile: MinecraftAPI.MinecraftProfile,
    override val token: MinecraftToken,
    val oAuth: MicrosoftAuthProvider.OAuthToken
) : AuthData()

@Serializable
data class MojangAuthData(
    override val profile: MinecraftAPI.MinecraftProfile,
    override val token: MinecraftToken
) : AuthData()

@Serializable
data class MinecraftToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") var expiry: Long
) {
    fun update() {
        expiry = System.currentTimeMillis() + (expiry * 1000)
    }
}

abstract class AuthProvider {

    abstract suspend fun login(current: AuthData? = null): AuthData

}

class AuthManager(root: File) {
    private val authFile = File(root, "auth.json")

    suspend fun login(): AuthData {
        val current = getCurrentData()
        val provider = current?.getProvider()
            ?: when (InputUtil.askSelection("Select an account type", "Microsoft", "Mojang")) {
                0 -> MicrosoftAuthProvider()
                1 -> MojangAuthProvider()
                else -> throw IllegalArgumentException("Invalid selection")
            }

        val data = provider.login(current)
        authFile.writeText(data.toJson())
        return data
    }

    private fun getCurrentData(): AuthData? {
        if (!authFile.parentFile.exists()) authFile.parentFile.mkdirs()
        return if (authFile.exists()) AuthData.fromJson(authFile.readText()) else null
    }

    private fun AuthData.getProvider() = when (this) {
        is MicrosoftAuthData -> MicrosoftAuthProvider()
        is MojangAuthData -> MojangAuthProvider()
    }

}