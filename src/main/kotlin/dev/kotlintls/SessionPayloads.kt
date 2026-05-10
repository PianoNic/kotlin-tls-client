package dev.kotlintls

data class DestroySessionPayload(val sessionId: String)
data class DestroySessionResponse(val id: String, val success: Boolean)

data class GetCookiesFromSessionPayload(val sessionId: String, val url: String)
data class GetCookiesFromSessionResponse(val id: String, val cookies: List<Cookie>)
