package dev.kotlintls.websocket

import com.google.gson.Gson
import dev.kotlintls.engine.TlsClientEngine
import dev.kotlintls.internal.WsFrame
import dev.kotlintls.internal.WsResultResponse
import java.io.IOException
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A persistent WebSocket connection backed by the Go tls-client engine. The
 * upgrade request goes through the same browser-shaped TLS handshake as
 * regular HTTP requests, so the connection is indistinguishable from a real
 * browser at the TLS layer.
 *
 * One JVM thread is dedicated to draining incoming frames per WebSocket. For
 * one-or-a-few connections this is cheap; not designed for thousands of
 * concurrent sockets in a single process.
 *
 * Construct via [dev.kotlintls.TlsClient.openWebSocket]. Closing is automatic
 * if the peer initiates it; otherwise call [close].
 */
class WebSocket internal constructor(
    private val engine: TlsClientEngine,
    private val connId: String,
    private val listener: WebSocketListener,
) {

    private val gson = Gson()
    private val running = AtomicBoolean(true)

    private val recvThread: Thread = Thread {
        try {
            listener.onOpen(this)
        } catch (t: Throwable) {
            failAndStop(t)
            return@Thread
        }
        recvLoop()
    }.apply {
        isDaemon = true
        name = "kotlintls-ws-recv-$connId"
        start()
    }

    /** Send a UTF-8 text frame. */
    fun send(text: String) {
        if (!running.get()) throw IOException("WebSocket is closed")
        val resp = gson.fromJson(engine.wsSend(connId, text, isBinary = false), WsResultResponse::class.java)
        if (resp == null || !resp.ok) throw IOException(resp?.error ?: "ws send failed")
    }

    /** Send a binary frame. */
    fun sendBinary(data: ByteArray) {
        if (!running.get()) throw IOException("WebSocket is closed")
        val b64 = Base64.getEncoder().encodeToString(data)
        val resp = gson.fromJson(engine.wsSend(connId, b64, isBinary = true), WsResultResponse::class.java)
        if (resp == null || !resp.ok) throw IOException(resp?.error ?: "ws sendBinary failed")
    }

    /**
     * Close cleanly. Sends a Close frame (default code 1000 / "normal closure")
     * and tears down the underlying connection. Idempotent.
     */
    fun close(code: Int = 1000, reason: String = "") {
        if (!running.compareAndSet(true, false)) return
        engine.wsClose(connId, code, reason)
        // The recv thread will exit on the next read returning a close/error frame.
    }

    /** True until the receive thread has stopped processing frames. */
    val isOpen: Boolean get() = running.get()

    private fun recvLoop() {
        while (running.get()) {
            val raw = try {
                // 0 = block forever; the Go side sets no deadline on the underlying conn.
                engine.wsRecv(connId, timeoutMs = 0)
            } catch (t: Throwable) {
                failAndStop(t); return
            }
            val frame = try {
                gson.fromJson(raw, WsFrame::class.java)
            } catch (t: Throwable) {
                failAndStop(IOException("malformed wsRecv response: ${raw.take(200)}", t)); return
            }

            try {
                when (frame.type) {
                    "text" -> frame.data?.let { listener.onMessage(this, it) }
                    "binary" -> frame.data?.let {
                        listener.onBinaryMessage(this, Base64.getDecoder().decode(it))
                    }
                    "close" -> {
                        running.set(false)
                        listener.onClosed(this, frame.code ?: 1006, frame.reason ?: "")
                        return
                    }
                    "error" -> {
                        failAndStop(IOException(frame.error ?: "unknown ws error"))
                        return
                    }
                    "timeout", "unknown", null -> { /* keep looping */ }
                }
            } catch (t: Throwable) {
                failAndStop(t)
                return
            }
        }
    }

    private fun failAndStop(t: Throwable) {
        if (running.compareAndSet(true, false)) {
            try {
                listener.onFailure(this, t)
            } catch (_: Throwable) {
                // Listeners shouldn't throw here, but if they do we don't want to crash the thread.
            }
            try {
                engine.wsClose(connId, 1006, "")
            } catch (_: Throwable) { /* best effort */ }
        }
    }
}
