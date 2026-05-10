package dev.kotlintls

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
}
