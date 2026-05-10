package dev.kotlintls.session

import dev.kotlintls.models.ResponseData

class Response(private val data: ResponseData) {
    val ok: Boolean get() = data.ok
    val status: Int get() = data.status
    val headers: Map<String, List<String>> get() = data.headers
    val body: String get() = data.body
    val cookies: Map<String, String> get() = data.cookies
    val url: String get() = data.target
    val sessionId: String? get() = data.sessionId
    val usedProtocol: String get() = data.usedProtocol

    fun text(): String = body
}
