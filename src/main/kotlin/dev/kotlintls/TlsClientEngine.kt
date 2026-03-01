package dev.kotlintls

/**
 * Engine that performs the actual HTTP request with optional TLS fingerprinting.
 * - [OkHttpTlsEngine]: default, uses OkHttp (no JA3 mimicry).
 * - [NativeTlsEngine]: delegates to Go tls-client .so via JNI (real JA3 when the native lib is present).
 */
interface TlsClientEngine {
    /** Performs request; input and output are JSON strings matching Go tls-client FFI. */
    fun request(requestJson: String): String
    fun destroySession(payloadJson: String): String
    fun getCookiesFromSession(payloadJson: String): String
    fun destroyAll(): String
}
