package dev.koding.launcher.auth

import dev.koding.launcher.util.httpClient
import dev.koding.launcher.util.respondResource
import dev.koding.launcher.util.sha256
import dev.koding.launcher.util.system.SwingUtil
import dev.koding.launcher.util.toUrlBase64
import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.awt.Desktop
import java.net.URI
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

const val CLIENT_ID = "92b1e10b-a6d8-4411-bcc8-d17c79c85aa7"
private const val SCOPES = "XboxLive.signin XboxLive.offline_access"
private const val REDIRECT_URI = "http://localhost:8080/"

class MicrosoftAuthProvider : AuthProvider() {

    private val logger = KotlinLogging.logger {}

    override suspend fun login(current: AuthData?): AuthData {
        val authData = current as? MicrosoftAuthData
        val oauth = authData?.oAuth ?: fetchOauthToken()

        if (oauth.expiry < System.currentTimeMillis()) {
            logger.info { "Refreshing Microsoft OAuth token" }
            SwingUtil.showError("Microsoft Login", "Your Microsoft login has expired. Please login again.")
            return fetchOauthToken().fetchLoginData()
        }

        if (authData != null && authData.token.expiry < System.currentTimeMillis()) {
            logger.info { "Refreshing Minecraft login token" }
            return oauth.fetchLoginData()
        }

        return authData ?: oauth.fetchLoginData()
    }

    private suspend fun fetchOauthToken(): OAuthToken {
        logger.debug { "Fetching Microsoft OAuth token" }
        val verifier = Random.nextBytes(32).toUrlBase64()
        val code = getAuthorizationCode(verifier) ?: error("Failed to get code")
        return getAuthorizationToken(
            "code" to code,
            "code_verifier" to verifier,
            "grant_type" to "authorization_code",
        )
    }

    private suspend fun OAuthToken.fetchLoginData(): MicrosoftAuthData {
        // Authenticating with Xbox
        logger.info { "Authenticating with Xbox" }
        val xblToken = getXBLToken(this)
        val xstsToken = getXSTSToken(xblToken)

        // Get Minecraft token & profile
        logger.info { "Getting Minecraft token & profile" }
        val mcToken = getMinecraftToken(xblToken, xstsToken)
        val mcProfile = MinecraftAPI.getMinecraftProfile(mcToken.accessToken)

        return MicrosoftAuthData(mcProfile, mcToken, this)
    }

    private suspend fun getAuthorizationCode(verifier: String): String? {
        val url = url {
            protocol = URLProtocol.HTTPS
            host = "login.live.com"
            encodedPath = "/oauth20_authorize.srf"
            parameters.apply {
                // Might not be needed to be a const?
                append("client_id", CLIENT_ID)
                append("scope", SCOPES)
                append("redirect_uri", REDIRECT_URI)
                append("response_type", "code")
                append("prompt", "select_account")
                append("code_challenge", verifier.sha256?.toUrlBase64() ?: "")
                append("code_challenge_method", "S256")
            }
        }
        logger.info { "Authenticate using $url" }

        if (Desktop.isDesktopSupported()) {
            withContext(Dispatchers.IO) {
                Desktop.getDesktop().browse(URI(url))
            }
        }

        logger.info { "Waiting for authorization code" }
        val code = suspendCoroutine<String> { cont ->
            embeddedServer(Netty, port = 8080) {
                routing {
                    static("css") {
                        resources("web/css")
                    }

                    get("/") {
                        val code = call.request.queryParameters["code"]
                            ?: return@get call.respond("Code not provided")

                        logger.debug { "Got code: $code" }
                        call.respondResource("/web/microsoft/success.html")
                        cont.resume(code)

                        delay(1000)
                        (environment as? ApplicationEngineEnvironment)?.stop()
                    }
                }
            }.start()
        }

        logger.debug { "Server shutdown" }
        return code
    }

    private suspend fun getAuthorizationToken(vararg params: Pair<String, String>): OAuthToken =
        httpClient.post<OAuthToken>("https://login.live.com/oauth20_token.srf") {
            header("Origin", "http://localhost")
            body = FormDataContent(
                Parameters.build {
                    params.forEach { (key, value) -> append(key, value) }
                    append("redirect_uri", REDIRECT_URI)
                    append("client_id", CLIENT_ID)
                    append("scope", SCOPES)
                }
            )
        }.apply { update() }

    private suspend fun getXBLToken(token: OAuthToken): XBLToken =
        httpClient.post("https://user.auth.xboxlive.com/user/authenticate") {
            contentType(ContentType.Application.Json)
            body = XBLTokenRequest(XBLTokenRequest.Properties("d=${token.accessToken}"))
        }

    private suspend fun getXSTSToken(token: XBLToken): XSTSToken =
        httpClient.post("https://xsts.auth.xboxlive.com/xsts/authorize") {
            contentType(ContentType.Application.Json)
            body = XSTSTokenRequest(XSTSTokenRequest.Properties(listOf(token.token)))
        }

    private suspend fun getMinecraftToken(xbl: XBLToken, xsts: XSTSToken): MinecraftToken =
        httpClient.post<MinecraftToken>("https://api.minecraftservices.com/authentication/login_with_xbox") {
            contentType(ContentType.Application.Json)
            body = MinecraftTokenRequest("XBL3.0 x=${xbl.userHash};${xsts.token}")
        }.apply { update() }

    @Serializable
    data class OAuthToken(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String,
        @SerialName("expires_in") var expiry: Long
    ) {
        fun update() {
            expiry = System.currentTimeMillis() + (expiry * 1000)
        }
    }

    @Serializable
    @Suppress("HttpUrlsUsage")
    data class XBLTokenRequest(
        @SerialName("Properties") val properties: Properties,
        @SerialName("RelyingParty") val relyingParty: String = "http://auth.xboxlive.com",
        @SerialName("TokenType") val tokenType: String = "JWT"
    ) {
        @Serializable
        data class Properties(
            @SerialName("RpsTicket") val rpsTicket: String,
            @SerialName("AuthMethod") val authMethod: String = "RPS",
            @SerialName("SiteName") val siteName: String = "user.auth.xboxlive.com"
        )
    }

    @Serializable
    data class XBLToken(
        @SerialName("Token") val token: String,
        @SerialName("DisplayClaims") val displayClaims: DisplayClaims
    ) {
        val userHash = displayClaims.xui.first().uhs

        @Serializable
        data class DisplayClaims(
            @SerialName("xui") val xui: List<XUI>
        )

        @Serializable
        data class XUI(
            @SerialName("uhs") val uhs: String
        )
    }

    @Serializable
    data class XSTSTokenRequest(
        @SerialName("Properties") val properties: Properties,
        @SerialName("RelyingParty") val relyingParty: String = "rp://api.minecraftservices.com/",
        @SerialName("TokenType") val tokenType: String = "JWT"
    ) {
        @Serializable
        data class Properties(
            @SerialName("UserTokens") val userTokens: List<String>,
            @SerialName("SandboxId") val sandboxId: String = "RETAIL"
        )
    }

    @Serializable
    data class XSTSToken(
        @SerialName("Token") val token: String
    )

    @Serializable
    data class MinecraftTokenRequest(
        val identityToken: String
    )
}