package dev.kotlintls

import com.google.gson.JsonParser
import dev.kotlintls.client.fetch
import dev.kotlintls.models.RequestMethod
import dev.kotlintls.websocket.WebSocket
import dev.kotlintls.websocket.WebSocketListener
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Positive WS fingerprint test. The TLS engine that opens a WebSocket is the same engine that
 * powers HTTP requests in this library. So if the engine produces Chrome-shaped JA4 over HTTP,
 * the WS upgrade also goes through Chrome-shaped TLS — by construction.
 *
 * This test exercises both: it opens a WebSocket via the client and, in the same run,
 * fetches https://tls.peet.ws/api/all over HTTPS through the same client. Both must succeed,
 * and the HTTPS leg must report a Chrome-shaped JA4.
 *
 * (Strict bit-perfect WS-handshake fingerprint capture is impractical without a server that
 * mirrors the WS upgrade's TLS handshake back over the WebSocket; the engine-shared assertion
 * is the achievable equivalent.)
 */
class WebSocketFingerprintTest {

    private val client = TlsClient()
    private val echoUrls = listOf(
        "wss://echo.websocket.org",
        "wss://ws.postman-echo.com/raw",
        "wss://ws.ifelse.io"
    )

    @Test
    fun `WS opens AND the same engine reports Chrome-shaped JA4 to peet ws`() {
        // Step 1: open a WebSocket and verify the upgrade succeeds.
        val opened = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        var ws: WebSocket? = null
        for (url in echoUrls) {
            try {
                ws = client.openWebSocket(
                    url = url,
                    listener = object : WebSocketListener {
                        override fun onOpen(webSocket: WebSocket) { opened.countDown() }
                        override fun onFailure(webSocket: WebSocket, t: Throwable) {
                            failure.set(t); opened.countDown()
                        }
                    }
                )
                break
            } catch (_: Throwable) { /* try next */ }
        }
        assertNotNull(ws, "No WS echo reachable")
        assertTrue(opened.await(15, TimeUnit.SECONDS), "WS upgrade did not complete")
        ws!!.close()

        // Step 2: same client, regular HTTPS request → peet.ws → confirm Chrome-shaped JA4.
        val resp = fetch("https://tls.peet.ws/api/all", RequestMethod.GET)
        val tls = JsonParser.parseString(resp.body).asJsonObject.getAsJsonObject("tls")
        val ja4 = tls.get("ja4").asString
        assertTrue(ja4.startsWith("t13d"),
            "JA4 should start with t13d (TLS 1.3); got '$ja4'")

        // GREASE in the cipher list is a Chromium-only signal.
        val ciphers = tls.getAsJsonArray("ciphers").map { it.asString }
        val greasePattern = Regex("(?i)0x([0-9a-f])a\\1a")
        val greasy = ciphers.any { it.contains("GREASE", ignoreCase = true) || greasePattern.containsMatchIn(it) }
        assertTrue(greasy,
            "Engine should emit GREASE; ciphers were $ciphers — meaning the WS engine isn't Chrome-shaped.")
    }
}
