package dev.kotlintls

import com.google.gson.Gson
import dev.kotlintls.engine.NativeTlsEngine
import dev.kotlintls.engine.TlsClientEngine
import dev.kotlintls.models.ClientIdentifier
import dev.kotlintls.models.DestroySessionPayload
import dev.kotlintls.models.DestroySessionResponse
import dev.kotlintls.models.GetCookiesFromSessionPayload
import dev.kotlintls.models.GetCookiesFromSessionResponse
import dev.kotlintls.models.RequestPayload
import dev.kotlintls.models.ResponseData
import dev.kotlintls.internal.WsOpenPayload
import dev.kotlintls.internal.WsOpenResponse
import dev.kotlintls.internal.parseDestroySessionResponse
import dev.kotlintls.internal.parseGetCookiesResponse
import dev.kotlintls.internal.parseResponseJson
import dev.kotlintls.internal.toJson
import dev.kotlintls.internal.toRequestJson
import dev.kotlintls.websocket.WebSocket
import dev.kotlintls.websocket.WebSocketListener
import java.io.IOException

class TlsClient @JvmOverloads constructor(
    private val engine: TlsClientEngine = NativeTlsEngine()
) {

    fun request(payload: RequestPayload): ResponseData {
        val json = payload.toRequestJson()
        val responseJson = engine.request(json)
        return responseJson.parseResponseJson()
    }

    fun destroySession(payload: DestroySessionPayload): DestroySessionResponse {
        val out = engine.destroySession(payload.toJson())
        return out.parseDestroySessionResponse()
    }

    fun getCookiesFromSession(payload: GetCookiesFromSessionPayload): GetCookiesFromSessionResponse {
        val out = engine.getCookiesFromSession(payload.toJson())
        return out.parseGetCookiesResponse()
    }

    fun destroyAll(): DestroySessionResponse {
        val out = engine.destroyAll()
        return out.parseDestroySessionResponse()
    }

    /**
     * Open a WebSocket. The TLS handshake uses the same browser-impersonating
     * profile as regular requests; pass a [headerOrder] to control the order of
     * the HTTP/1.1 Upgrade headers (matters for fingerprint matching against
     * real Chrome — Spotify, Cloudflare, Akamai all check this).
     *
     * @param url Required, must start with `wss://` or `ws://`.
     * @param headers Headers to send on the upgrade request (e.g. Origin, User-Agent, Cache-Control).
     * @param headerOrder If non-empty, headers are emitted on the wire in this order
     *  (lower-cased to match Chrome's HTTP/1.1 emission). Names not in [headers] are silently dropped.
     * @param clientIdentifier TLS profile (default Chrome 133).
     * @param handshakeTimeoutMs Time budget for the upgrade handshake.
     * @param withRandomTLSExtensionOrder Randomize TLS extension order to avoid trivial static-fingerprint detection.
     * @param insecureSkipVerify Skip TLS certificate verification (test/dev only).
     * @param proxyUrl Optional HTTP proxy URL.
     * @param listener Callback receiver. Methods are invoked on the WebSocket's own receive thread.
     */
    @JvmOverloads
    fun openWebSocket(
        url: String,
        headers: Map<String, String> = emptyMap(),
        headerOrder: List<String> = emptyList(),
        clientIdentifier: ClientIdentifier = ClientIdentifier.DEFAULT,
        handshakeTimeoutMs: Int = 30_000,
        withRandomTLSExtensionOrder: Boolean = true,
        insecureSkipVerify: Boolean = false,
        proxyUrl: String? = null,
        listener: WebSocketListener,
    ): WebSocket {
        val payload = WsOpenPayload(
            url = url,
            headers = headers,
            headerOrder = headerOrder,
            tlsClientIdentifier = clientIdentifier.value,
            withRandomTLSExtensionOrder = withRandomTLSExtensionOrder,
            handshakeTimeoutMilliseconds = handshakeTimeoutMs,
            insecureSkipVerify = insecureSkipVerify,
            proxyUrl = proxyUrl,
        )
        val raw = engine.wsOpen(Gson().toJson(payload))
        val resp = Gson().fromJson(raw, WsOpenResponse::class.java)
            ?: throw IOException("ws open returned no response")
        if (!resp.ok || resp.connId == null) {
            throw IOException(resp.error ?: "ws open failed")
        }
        return WebSocket(engine, resp.connId, listener)
    }
}
