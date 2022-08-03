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

package dev.koding.launcher.auth

import dev.koding.launcher.util.json
import dev.koding.launcher.util.system.SwingUtil
import dev.koding.launcher.util.toJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.io.File

@Serializable
sealed class AuthData {
    abstract val profile: MinecraftAPI.MinecraftProfile
    abstract val token: MinecraftToken
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
    private val logger = KotlinLogging.logger {}
    private val authFile = File(root, "auth.json")

    suspend fun login(): AuthData {
        val current = getCurrentData()
        logger.debug { "Current auth data: $current" }

        val provider = current?.getProvider()
            ?: when (SwingUtil.askSelection("Select an account type", "Microsoft", "Mojang")) {
                "Microsoft" -> MicrosoftAuthProvider()
                "Mojang" -> MojangAuthProvider()
                else -> throw IllegalArgumentException("Invalid selection")
            }

        logger.info { "Selected provider: ${provider::class.java.simpleName}" }
        val data = provider.login(current)
        authFile.writeText(data.toJson())

        logger.debug { "New auth data: $data" }
        return data
    }

    private fun getCurrentData(): AuthData? {
        if (!authFile.parentFile.exists()) authFile.parentFile.mkdirs()
        return if (authFile.exists()) authFile.json() else null
    }

    private fun AuthData.getProvider() = when (this) {
        is MicrosoftAuthData -> MicrosoftAuthProvider()
        is MojangAuthData -> MojangAuthProvider()
    }

}