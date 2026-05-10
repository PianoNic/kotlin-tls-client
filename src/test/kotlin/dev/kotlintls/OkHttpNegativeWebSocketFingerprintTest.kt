package dev.kotlintls

import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Negative baseline. OkHttp's WebSocket client uses the JDK TLS stack, which provably does
 * NOT emit a Chrome-shaped TLS handshake. This test proves it.
 *
 * The shape mirrors the positive [WebSocketFingerprintTest]: open a WS, then make a parallel
 * HTTPS request to tls.peet.ws via the same OkHttp instance, and assert the engine's JA4 is
 * NOT Chrome-shaped.
 *
 * If this test ever starts failing (i.e. OkHttp suddenly produces Chrome-shaped JA4), the
 * positive assertions in [WebSocketFingerprintTest] / [TlsFingerprintTest] are tautologies
 * and we want to know.
 */
class OkHttpNegativeWebSocketFingerprintTest {

    private val client = OkHttpClient.Builder().build()
    private val echoUrls = listOf(
        "wss://echo.websocket.org",
        "wss://ws.postman-echo.com/raw",
        "wss://ws.ifelse.io"
    )

    @Test
    fun `OkHttp WS opens but the same client reports a non-Chrome JA4 to peet ws`() {
        // Step 1: OkHttp WebSocket upgrade — should succeed.
        val opened = CountDownLatch(1)
        var ws: WebSocket? = null
        for (url in echoUrls) {
            val attempt = CountDownLatch(1)
            val candidate = client.newWebSocket(
                Request.Builder().url(url).build(),
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                        opened.countDown(); attempt.countDown()
                    }
                    override fun onFailure(
                        webSocket: WebSocket, t: Throwable, response: okhttp3.Response?
                    ) { attempt.countDown() }
                }
            )
            if (attempt.await(10, TimeUnit.SECONDS) && opened.count == 0L) {
                ws = candidate
                break
            } else {
                candidate.cancel()
            }
        }
        assertNotNull(ws, "No WS echo reachable for OkHttp")
        ws!!.close(1000, "")

        // Step 2: HTTPS request to peet.ws via OkHttp → expect JDK-shaped JA4 (not Chrome).
        val req = Request.Builder().url("https://tls.peet.ws/api/all").build()
        val body = client.newCall(req).execute().use { it.body!!.string() }
        val tls = JsonParser.parseString(body).asJsonObject.getAsJsonObject("tls")
        val ja4 = tls.get("ja4").asString

        // Chrome 133's first segment is 't13d1516h2'. OkHttp/JDK won't produce that combination.
        assertFalse(
            ja4.startsWith("t13d1516h2"),
            "OkHttp JA4 ('$ja4') happens to share Chrome 133's leading segment. Either OkHttp " +
                "shipped a fingerprint match (extremely unlikely) or this test needs tightening."
        )

        // Cipher list must NOT contain GREASE (Chromium-only signal).
        val ciphers = tls.getAsJsonArray("ciphers").map { it.asString }
        val greasePattern = Regex("(?i)0x([0-9a-f])a\\1a")
        val greasy = ciphers.any { it.contains("GREASE", ignoreCase = true) || greasePattern.containsMatchIn(it) }
        assertFalse(
            greasy,
            "OkHttp must not produce GREASE in the cipher list (proves our 'GREASE present' " +
                "assertion in WebSocketFingerprintTest is meaningful). Got: $ciphers"
        )
    }
}
