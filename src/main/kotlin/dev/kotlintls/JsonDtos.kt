package dev.kotlintls

import com.google.gson.annotations.SerializedName

internal data class RequestInputJson(
    val requestUrl: String,
    val requestMethod: String,
    val requestBody: String? = null,
    val requestCookies: List<CookieJson>? = null,
    val tlsClientIdentifier: String? = null,
    val customTlsClient: CustomTlsClientJson? = null,
    val sessionId: String? = null,
    val followRedirects: Boolean = false,
    val insecureSkipVerify: Boolean = false,
    val isByteResponse: Boolean = false,
    val isByteRequest: Boolean = false,
    val withoutCookieJar: Boolean = false,
    val withCustomCookieJar: Boolean = false,
    val withRandomTLSExtensionOrder: Boolean = false,
    val timeoutSeconds: Int = 30,
    val timeoutMilliseconds: Int = 0,
    val proxyUrl: String? = null,
    val headers: Map<String, String>? = null,
    val headerOrder: List<String>? = null,
    val defaultHeaders: Map<String, List<String>>? = null,
    val connectHeaders: Map<String, List<String>>? = null,
    val forceHttp1: Boolean = false,
    val disableHttp3: Boolean = false,
    val withProtocolRacing: Boolean = false,
    val disableIPV6: Boolean = false,
    val disableIPV4: Boolean = false,
    val localAddress: String? = null,
    val serverNameOverwrite: String? = null,
    val requestHostOverride: String? = null,
    val certificatePinningHosts: Map<String, List<String>>? = null,
    val catchPanics: Boolean = false,
    val withDebug: Boolean = false,
    val isRotatingProxy: Boolean = false,
)

internal data class CookieJson(
    val name: String,
    val value: String,
    val path: String = "/",
    val domain: String = "",
    val expires: Long = 0L,
    val maxAge: Int = -1,
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
)

internal data class CustomTlsClientJson(
    val ja3String: String,
    val h2Settings: Map<String, Int>? = null,
    val h2SettingsOrder: List<String>? = null,
    val pseudoHeaderOrder: List<String>? = null,
    val connectionFlow: Int = 0,
    val priorityFrames: List<PriorityFrameJson>? = null,
    val headerPriority: PriorityParamJson? = null,
    val certCompressionAlgos: List<String>? = null,
    val keyShareCurves: List<String>? = null,
    val supportedSignatureAlgorithms: List<String>? = null,
    val supportedVersions: List<String>? = null,
)

internal data class PriorityParamJson(val streamDep: Int, val exclusive: Boolean, val weight: Int)
internal data class PriorityFrameJson(val streamID: Int, val priorityParam: PriorityParamJson)

internal data class ResponseJson(
    val id: String? = null,
    val status: Int? = null,
    val body: String? = null,
    val headers: Map<String, List<String>>? = null,
    val cookies: Map<String, String>? = null,
    val target: String? = null,
    val sessionId: String? = null,
    @SerializedName("usedProtocol") val usedProtocol: String? = null,
)

internal data class GetCookiesResponseJson(val id: String?, val cookies: List<CookieJson>?)
