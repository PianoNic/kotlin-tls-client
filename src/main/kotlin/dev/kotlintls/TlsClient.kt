package dev.kotlintls

import java.util.UUID

/**
 * HTTP client with TLS fingerprint impersonation via the Go tls-client library.
 *
 * All requests go through the native Go library via JNA — no fallback.
 * Use TlsClient() for the default engine, or TlsClient(customEngine) for a custom one.
 */
class TlsClient @JvmOverloads constructor(
    private val engine: TlsClientEngine = NativeTlsEngine()
) {

    /**
     * Synchronous request via the Go tls-client engine.
     */
    fun request(payload: RequestPayload): ResponseData {
        val json = payload.toRequestJson()
        val responseJson = engine.request(json)
        return responseJson.parseResponseJson()
    }

    /**
     * Destroy a session.
     */
    fun destroySession(payload: DestroySessionPayload): DestroySessionResponse {
        val out = engine.destroySession(payload.toJson())
        return com.google.gson.Gson().fromJson(out, DestroySessionResponse::class.java)
    }

    /**
     * Get cookies for a URL from a session.
     */
    fun getCookiesFromSession(payload: GetCookiesFromSessionPayload): GetCookiesFromSessionResponse {
        val out = engine.getCookiesFromSession(payload.toJson())
        return out.parseGetCookiesResponse()
    }

    /**
     * Destroy all sessions.
     */
    fun destroyAll(): DestroySessionResponse {
        val out = engine.destroyAll()
        return com.google.gson.Gson().fromJson(out, DestroySessionResponse::class.java)
    }
}
