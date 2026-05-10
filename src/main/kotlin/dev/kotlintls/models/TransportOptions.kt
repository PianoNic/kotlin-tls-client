package dev.kotlintls.models

data class TransportOptions(
    val idleConnTimeout: Long? = null,
    val maxIdleConns: Int = 0,
    val maxIdleConnsPerHost: Int = 0,
    val maxConnsPerHost: Int = 0,
    val maxResponseHeaderBytes: Long = 0L,
    val writeBufferSize: Int = 0,
    val readBufferSize: Int = 0,
    val disableKeepAlives: Boolean = false,
    val disableCompression: Boolean = false
)
