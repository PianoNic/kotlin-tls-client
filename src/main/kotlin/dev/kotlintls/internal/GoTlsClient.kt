package dev.kotlintls.internal

import com.sun.jna.Library
import com.sun.jna.Native

/**
 * JNA interface to the Go tls-client shared library.
 * All functions take/return C strings (JSON-encoded payloads).
 */
internal interface GoTlsClient : Library {
    // HTTP
    fun request(payload: String): String
    fun destroySession(payload: String): String
    fun getCookiesFromSession(payload: String): String
    fun destroyAll(): String

    // WebSocket
    fun wsOpen(payload: String): String
    fun wsSend(connId: String, message: String, isBinary: Int): String
    fun wsRecv(connId: String, timeoutMs: Int): String
    fun wsClose(connId: String, code: Int, reason: String): String

    companion object {
        // Decode the Go library's char* request/response strings as UTF-8 rather than the JVM's
        // platform default charset. Without this, a runtime whose default isn't UTF-8 (e.g. some
        // Android devices) mangles multi-byte UTF-8 in response bodies into mojibake.
        private val utf8Options = mapOf(Library.OPTION_STRING_ENCODING to "UTF-8")

        fun load(libPath: String): GoTlsClient =
            Native.load(libPath, GoTlsClient::class.java, utf8Options) as GoTlsClient

        fun loadSystem(libName: String): GoTlsClient =
            Native.load(libName, GoTlsClient::class.java, utf8Options) as GoTlsClient
    }
}
