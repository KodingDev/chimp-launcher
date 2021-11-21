package dev.koding.launcher.auth

import dev.koding.launcher.util.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

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
data class MinecraftToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") var expiry: Long
) {
    fun update() {
        expiry = System.currentTimeMillis() + (expiry * 1000)
    }
}