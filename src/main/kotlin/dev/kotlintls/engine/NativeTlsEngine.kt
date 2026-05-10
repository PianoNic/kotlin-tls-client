package dev.kotlintls.engine

import dev.kotlintls.internal.GoTlsClient
import dev.kotlintls.internal.NativeLibLoader

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

    override fun wsOpen(payloadJson: String): String = lib.wsOpen(payloadJson)

    override fun wsSend(connId: String, message: String, isBinary: Boolean): String =
        lib.wsSend(connId, message, if (isBinary) 1 else 0)

    override fun wsRecv(connId: String, timeoutMs: Int): String = lib.wsRecv(connId, timeoutMs)

    override fun wsClose(connId: String, code: Int, reason: String): String =
        lib.wsClose(connId, code, reason)
}
