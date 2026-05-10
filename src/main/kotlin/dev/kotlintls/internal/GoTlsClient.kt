package dev.kotlintls.internal

import com.sun.jna.Library
import com.sun.jna.Native

/**
 * JNA interface to the Go tls-client shared library.
 * All 4 functions take/return C strings (JSON).
 */
internal interface GoTlsClient : Library {
    fun request(payload: String): String
    fun destroySession(payload: String): String
    fun getCookiesFromSession(payload: String): String
    fun destroyAll(): String

    companion object {
        fun load(libPath: String): GoTlsClient =
            Native.load(libPath, GoTlsClient::class.java) as GoTlsClient

        fun loadSystem(libName: String): GoTlsClient =
            Native.load(libName, GoTlsClient::class.java) as GoTlsClient
    }
}
