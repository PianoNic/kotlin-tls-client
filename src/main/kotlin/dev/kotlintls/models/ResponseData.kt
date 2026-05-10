package dev.kotlintls.models

data class ResponseData(
    val status: Int,
    val body: String,
    val headers: Map<String, List<String>>,
    val cookies: Map<String, String>,
    val target: String,
    val sessionId: String? = null,
    val usedProtocol: String = "HTTP/1.1",
    val id: String? = null
) {
    val ok: Boolean get() = status in 200..299
}
