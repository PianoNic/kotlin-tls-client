package dev.kotlintls

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.kotlintls.engine.TlsClientEngine
import dev.kotlintls.websocket.WebSocket
import dev.kotlintls.websocket.WebSocketListener
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.Base64
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Pure-unit tests for the WebSocket wrapper. Replaces the engine with a recording fake so the
 * tests verify dispatch logic, JSON shapes, and close lifecycle without touching the network.
 */
class WebSocketUnitTest {

    private val gson = Gson()

    /** Records every FFI call and lets tests script the wsRecv responses. */
    private class FakeEngine : TlsClientEngine {
        val openCalls = mutableListOf<String>()
        val sendCalls = ConcurrentLinkedQueue<Triple<String, String, Boolean>>()
        val closeCalls = mutableListOf<Triple<String, Int, String>>()
        val recvQueue = LinkedBlockingQueue<String>()
        var connId = "test-conn-1"

        override fun request(requestJson: String): String = error("not used")
        override fun destroySession(payloadJson: String): String = error("not used")
        override fun getCookiesFromSession(payloadJson: String): String = error("not used")
        override fun destroyAll(): String = error("not used")

        override fun wsOpen(payloadJson: String): String {
            openCalls += payloadJson
            return """{"ok":true,"connId":"$connId","id":"r1"}"""
        }

        override fun wsSend(connId: String, message: String, isBinary: Boolean): String {
            sendCalls += Triple(connId, message, isBinary)
            return """{"ok":true,"id":"r2"}"""
        }

        override fun wsRecv(connId: String, timeoutMs: Int): String {
            // Returns scripted frames; if queue is empty, blocks the recv thread (test
            // can drive close() to unblock and end the loop).
            return recvQueue.poll(30, TimeUnit.SECONDS) ?: """{"type":"timeout","id":"r3"}"""
        }

        override fun wsClose(connId: String, code: Int, reason: String): String {
            closeCalls += Triple(connId, code, reason)
            // Unblock any waiting wsRecv with a close frame.
            recvQueue.offer("""{"type":"close","code":$code,"reason":"$reason","id":"rC"}""")
            return """{"ok":true,"id":"rC2"}"""
        }
    }

    @Test
    fun `open serializes the payload correctly`() {
        val engine = FakeEngine()
        val client = TlsClient(engine)

        val ws = client.openWebSocket(
            url = "wss://example.com/socket",
            headers = mapOf("Origin" to "https://example.com"),
            headerOrder = listOf("origin", "user-agent"),
            handshakeTimeoutMs = 7777,
            listener = object : WebSocketListener {}
        )
        ws.close()

        assertEquals(1, engine.openCalls.size)
        val sent = JsonParser.parseString(engine.openCalls.first()).asJsonObject
        assertEquals("wss://example.com/socket", sent.get("url").asString)
        assertEquals("https://example.com", sent.getAsJsonObject("headers").get("Origin").asString)
        assertEquals("origin", sent.getAsJsonArray("headerOrder").get(0).asString)
        assertEquals(7777, sent.get("handshakeTimeoutMilliseconds").asInt)
    }

    @Test
    fun `onOpen fires before any onMessage`() {
        val engine = FakeEngine()
        val order = ConcurrentLinkedQueue<String>()
        val opened = CountDownLatch(1)
        val received = CountDownLatch(1)

        engine.recvQueue.offer("""{"type":"text","data":"hello","id":"f1"}""")

        val ws = TlsClient(engine).openWebSocket(
            url = "wss://x",
            listener = object : WebSocketListener {
                override fun onOpen(webSocket: WebSocket) { order += "open"; opened.countDown() }
                override fun onMessage(webSocket: WebSocket, text: String) { order += "msg:$text"; received.countDown() }
            }
        )
        try {
            assertTrue(opened.await(2, TimeUnit.SECONDS))
            assertTrue(received.await(2, TimeUnit.SECONDS))
            assertEquals(listOf("open", "msg:hello"), order.toList())
        } finally {
            ws.close()
        }
    }

    @Test
    fun `binary frames are base64-decoded before dispatch`() {
        val engine = FakeEngine()
        val raw = byteArrayOf(0x01, 0x02, 0x03, 0xFF.toByte())
        val b64 = Base64.getEncoder().encodeToString(raw)
        engine.recvQueue.offer("""{"type":"binary","data":"$b64","id":"f2"}""")

        val received = AtomicReference<ByteArray?>()
        val latch = CountDownLatch(1)
        val ws = TlsClient(engine).openWebSocket(
            url = "wss://x",
            listener = object : WebSocketListener {
                override fun onBinaryMessage(webSocket: WebSocket, data: ByteArray) {
                    received.set(data); latch.countDown()
                }
            }
        )
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertNotNull(received.get())
            assertTrue(raw.contentEquals(received.get()))
        } finally {
            ws.close()
        }
    }

    @Test
    fun `close frame from server invokes onClosed and stops the loop`() {
        val engine = FakeEngine()
        engine.recvQueue.offer("""{"type":"close","code":1006,"reason":"abnormal","id":"f3"}""")

        val closedCode = AtomicReference<Int?>()
        val closedReason = AtomicReference<String?>()
        val latch = CountDownLatch(1)
        val ws = TlsClient(engine).openWebSocket(
            url = "wss://x",
            listener = object : WebSocketListener {
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    closedCode.set(code); closedReason.set(reason); latch.countDown()
                }
            }
        )

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertEquals(1006, closedCode.get())
        assertEquals("abnormal", closedReason.get())
        assertFalse(ws.isOpen)
    }

    @Test
    fun `error frame invokes onFailure and stops the loop`() {
        val engine = FakeEngine()
        engine.recvQueue.offer("""{"type":"error","error":"connection reset","id":"f4"}""")

        val err = AtomicReference<Throwable?>()
        val latch = CountDownLatch(1)
        val ws = TlsClient(engine).openWebSocket(
            url = "wss://x",
            listener = object : WebSocketListener {
                override fun onFailure(webSocket: WebSocket, t: Throwable) { err.set(t); latch.countDown() }
            }
        )

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertNotNull(err.get())
        assertTrue(err.get()!!.message!!.contains("connection reset"))
        assertFalse(ws.isOpen)
    }

    @Test
    fun `send after close throws`() {
        val engine = FakeEngine()
        val ws = TlsClient(engine).openWebSocket(
            url = "wss://x",
            listener = object : WebSocketListener {}
        )
        ws.close()
        // Wait for the close frame to flow through the recv loop.
        Thread.sleep(100)

        val ex = assertThrows(IOException::class.java) { ws.send("nope") }
        assertTrue(ex.message!!.contains("closed"))
    }

    @Test
    fun `send forwards text to engine with isBinary=false`() {
        val engine = FakeEngine()
        val ws = TlsClient(engine).openWebSocket(
            url = "wss://x",
            listener = object : WebSocketListener {}
        )

        ws.send("payload-A")
        ws.close()

        val sent = engine.sendCalls.first { it.second == "payload-A" }
        assertEquals(false, sent.third)
    }

    @Test
    fun `sendBinary forwards base64-encoded bytes with isBinary=true`() {
        val engine = FakeEngine()
        val ws = TlsClient(engine).openWebSocket(
            url = "wss://x",
            listener = object : WebSocketListener {}
        )

        val data = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        ws.sendBinary(data)
        ws.close()

        val sent = engine.sendCalls.first { it.third }
        assertEquals(Base64.getEncoder().encodeToString(data), sent.second)
    }

    @Test
    fun `close is idempotent`() {
        val engine = FakeEngine()
        val ws = TlsClient(engine).openWebSocket(
            url = "wss://x",
            listener = object : WebSocketListener {}
        )

        ws.close(1000, "first")
        ws.close(1001, "second")
        Thread.sleep(50)

        // Only the first close should reach the engine.
        assertEquals(1, engine.closeCalls.size)
        assertEquals(1000, engine.closeCalls.first().second)
    }

    @Test
    fun `wsOpen failure throws IOException with the engine's error message`() {
        val engine = object : TlsClientEngine {
            override fun request(requestJson: String): String = error("nope")
            override fun destroySession(payloadJson: String): String = error("nope")
            override fun getCookiesFromSession(payloadJson: String): String = error("nope")
            override fun destroyAll(): String = error("nope")
            override fun wsOpen(payloadJson: String): String =
                """{"ok":false,"error":"server hung up","id":"e1"}"""
            override fun wsSend(connId: String, message: String, isBinary: Boolean): String = error("not reached")
            override fun wsRecv(connId: String, timeoutMs: Int): String = error("not reached")
            override fun wsClose(connId: String, code: Int, reason: String): String = error("not reached")
        }

        val ex = assertThrows(IOException::class.java) {
            TlsClient(engine).openWebSocket(url = "wss://x", listener = object : WebSocketListener {})
        }
        assertTrue(ex.message!!.contains("server hung up"))
    }

    @Test
    fun `listener that throws on a frame triggers onFailure and stops the loop`() {
        val engine = FakeEngine()
        engine.recvQueue.offer("""{"type":"text","data":"crash-me","id":"f5"}""")

        val failed = AtomicBoolean(false)
        val latch = CountDownLatch(1)
        val ws = TlsClient(engine).openWebSocket(
            url = "wss://x",
            listener = object : WebSocketListener {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    throw RuntimeException("listener boom")
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable) {
                    failed.set(true); latch.countDown()
                }
            }
        )

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertTrue(failed.get())
        assertFalse(ws.isOpen)
    }
}
