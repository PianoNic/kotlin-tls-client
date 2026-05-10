package dev.kotlintls.websocket

/**
 * Callback contract for [WebSocket]. All methods are called on the WebSocket's own
 * dedicated receive thread, so they should not block for long; offload heavy work
 * to your own executor / coroutine scope.
 *
 * Open this with [dev.kotlintls.TlsClient.openWebSocket].
 */
interface WebSocketListener {

    /** Called once after the upgrade handshake completes successfully. */
    fun onOpen(webSocket: WebSocket) {}

    /** A text frame arrived. */
    fun onMessage(webSocket: WebSocket, text: String) {}

    /** A binary frame arrived. */
    fun onBinaryMessage(webSocket: WebSocket, data: ByteArray) {}

    /** The peer closed the connection. After this, the receive thread exits. */
    fun onClosed(webSocket: WebSocket, code: Int, reason: String) {}

    /**
     * The connection failed. After this, the receive thread exits and you can no
     * longer send. Reasons include network errors, malformed frames, or any
     * exception thrown from a listener method.
     */
    fun onFailure(webSocket: WebSocket, t: Throwable) {}
}
