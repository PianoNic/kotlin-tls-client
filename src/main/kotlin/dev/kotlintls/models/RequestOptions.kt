package dev.kotlintls.models

data class RequestOptions(
    val headers: Map<String, String> = emptyMap(),
    val connectHeaders: Map<String, List<String>>? = null,
    val headerOrder: List<String> = emptyList(),
    val followRedirects: Boolean? = null,
    val proxy: String? = null,
    val isRotatingProxy: Boolean? = null,
    val cookies: Map<String, String> = emptyMap(),
    val byteResponse: Boolean = false,
    val byteRequest: Boolean = false,
    val hostOverride: String? = null,
    val body: String? = null
)
