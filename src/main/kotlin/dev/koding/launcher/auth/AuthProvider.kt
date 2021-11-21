package dev.koding.launcher.auth

import java.io.File

abstract class AuthProvider(root: File) {

    private val authFile = File(root, "auth.json")

    abstract suspend fun login(current: AuthData? = null): AuthData

    suspend fun login(): AuthData {
        val current = if (authFile.exists()) AuthData.fromJson(authFile.readText()) else null
        val data = login(current)
        authFile.writeText(data.toJson())
        return data
    }

}