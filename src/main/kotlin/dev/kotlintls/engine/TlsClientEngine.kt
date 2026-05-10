package dev.kotlintls.engine

/**
 * Engine that performs the actual HTTP request with TLS fingerprinting.
 * Default implementation: [NativeTlsEngine], which calls the Go tls-client shared library via JNA.
 *
 * Input and output are JSON strings matching the Go tls-client FFI.
 */
interface TlsClientEngine {
    fun request(requestJson: String): String
    fun destroySession(payloadJson: String): String
    fun getCookiesFromSession(payloadJson: String): String
    fun destroyAll(): String

    // WebSocket. Default impls throw so older fakes/test doubles still compile.
    fun wsOpen(payloadJson: String): String =
        throw UnsupportedOperationException("WebSocket not supported by this engine")

    fun wsSend(connId: String, message: String, isBinary: Boolean): String =
        throw UnsupportedOperationException("WebSocket not supported by this engine")

    fun wsRecv(connId: String, timeoutMs: Int): String =
        throw UnsupportedOperationException("WebSocket not supported by this engine")

    fun wsClose(connId: String, code: Int, reason: String): String =
        throw UnsupportedOperationException("WebSocket not supported by this engine")
}
