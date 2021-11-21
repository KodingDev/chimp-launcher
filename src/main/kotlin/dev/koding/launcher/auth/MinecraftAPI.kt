package dev.koding.launcher.auth

import dev.koding.launcher.util.httpClient
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

object MinecraftAPI {

    suspend fun getMinecraftProfile(token: String): MinecraftProfile =
        httpClient.get("https://api.minecraftservices.com/minecraft/profile") {
            header("Authorization", "Bearer $token")
        }

    @Serializable
    data class MinecraftProfile(
        val id: String,
        val name: String
    )

}