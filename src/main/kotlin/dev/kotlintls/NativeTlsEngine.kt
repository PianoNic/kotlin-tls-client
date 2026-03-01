package dev.kotlintls

/**
 * Uses the Go tls-client library so your requests look like a real browser (Chrome, Firefox, etc.).
 *
 * The native libraries are bundled inside the JAR and extracted automatically on first use.
 * No manual setup required — just use TlsClient(NativeTlsEngine()).
 *
 * Supported platforms: Android arm64-v8a, armeabi-v7a.
 * On other platforms the libraries must be pre-loaded via System.loadLibrary().
 */
class NativeTlsEngine : TlsClientEngine {

    init {
        NativeLibLoader.ensureLoaded()
    }

    override fun request(requestJson: String): String = nativeRequest(requestJson)
    override fun destroySession(payloadJson: String): String = nativeDestroySession(payloadJson)
    override fun getCookiesFromSession(payloadJson: String): String = nativeGetCookiesFromSession(payloadJson)
    override fun destroyAll(): String = nativeDestroyAll()

    private external fun nativeRequest(requestJson: String): String
    private external fun nativeDestroySession(payloadJson: String): String
    private external fun nativeGetCookiesFromSession(payloadJson: String): String
    private external fun nativeDestroyAll(): String
}
