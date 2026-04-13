package dev.kotlintls

/**
 * Uses the Go tls-client library so your requests look like a real browser (Chrome, Firefox, etc.).
 *
 * The native Go library is bundled inside the JAR and loaded automatically via JNA on first use.
 * No JNI bridge or manual setup required — just use TlsClient(NativeTlsEngine()).
 *
 * Supported platforms: Linux (x86_64, aarch64), macOS (arm64, x86_64), Windows (x86_64).
 * Android: arm64-v8a, armeabi-v7a (via System.loadLibrary).
 */
class NativeTlsEngine : TlsClientEngine {

    private val lib: GoTlsClient = NativeLibLoader.ensureLoaded()

    override fun request(requestJson: String): String = lib.request(requestJson)
    override fun destroySession(payloadJson: String): String = lib.destroySession(payloadJson)
    override fun getCookiesFromSession(payloadJson: String): String = lib.getCookiesFromSession(payloadJson)
    override fun destroyAll(): String = lib.destroyAll()
}
