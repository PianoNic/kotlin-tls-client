package dev.kotlintls

import dev.kotlintls.websocket.WebSocket
import dev.kotlintls.websocket.WebSocketListener
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Live end-to-end test: opens a WebSocket to a public echo server, sends a
 * message, expects the same message back. Confirms the new WS engine round-trips
 * through the impersonating TLS handshake without errors.
 *
 * Network-dependent like our other tests; same trade-off.
 */
class WebSocketTest {

    private val client = TlsClient()

    // Public WS echo servers — try in order; the test passes on the first one that
    // accepts the upgrade. This insulates the suite from any single host going down
    // or blocking CI runner IPs.
    private val echoUrls = listOf(
        "wss://echo.websocket.org",
        "wss://ws.postman-echo.com/raw",
        "wss://ws.ifelse.io"
    )

    @Test
    fun `echoes a text message`() {
        val opened = CountDownLatch(1)
        val received = CountDownLatch(1)
        val message = AtomicReference<String?>()
        val failure = AtomicReference<Throwable?>()

        var ws: WebSocket? = null
        var lastError: Throwable? = null
        for (url in echoUrls) {
            try {
                ws = client.openWebSocket(
                    url = url,
                    headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"
                    ),
                    listener = object : WebSocketListener {
                        override fun onOpen(webSocket: WebSocket) {
                            opened.countDown()
                            webSocket.send("ping-from-kotlin-tls-client")
                        }
                        override fun onMessage(webSocket: WebSocket, text: String) {
                            if (text.contains("ping-from-kotlin-tls-client")) {
                                message.set(text)
                                received.countDown()
                            }
                            // Some echos emit a banner; ignore frames that don't echo our payload.
                        }
                        override fun onFailure(webSocket: WebSocket, t: Throwable) {
                            failure.set(t)
                            opened.countDown(); received.countDown()
                        }
                    }
                )
                break
            } catch (t: Throwable) {
                lastError = t
            }
        }
        if (ws == null) throw AssertionError("No WS echo server reachable; lastError=$lastError")

        try {
            assertTrue(opened.await(15, TimeUnit.SECONDS), "onOpen did not fire within 15s; failure=${failure.get()}")
            assertNotNull(ws, "Open returned null")
            assertTrue(received.await(15, TimeUnit.SECONDS), "Echo did not arrive within 15s; failure=${failure.get()}")
            assertTrue(message.get()!!.contains("ping-from-kotlin-tls-client"))
        } finally {
            ws.close()
        }
    }
}
