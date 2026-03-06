package dev.kotlintls

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

private val gson = Gson()

/** Request payload as sent to Go FFI (camelCase to match Go json tags). */
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

/** Response as returned by Go FFI. */
internal data class ResponseJson(
    val id: String? = null,
    val status: Int,
    val body: String,
    val headers: Map<String, List<String>>? = null,
    val cookies: Map<String, String>? = null,
    val target: String? = null,
    val sessionId: String? = null,
    @SerializedName("usedProtocol") val usedProtocol: String? = null,
)

internal fun RequestPayload.toRequestInputJson(): RequestInputJson {
    return RequestInputJson(
        requestUrl = requestUrl,
        requestMethod = requestMethod.name,
        requestBody = requestBody,
        requestCookies = requestCookies.map { c ->
            CookieJson(c.name, c.value, c.path, c.domain, c.expires, c.maxAge, c.secure, c.httpOnly)
        }.takeIf { it.isNotEmpty() },
        tlsClientIdentifier = tlsClientIdentifier,
        customTlsClient = customTlsClient?.let { ct ->
            CustomTlsClientJson(
                ja3String = ct.ja3String,
                h2Settings = ct.h2Settings.mapValues { it.value.toInt() }.takeIf { it.isNotEmpty() },
                h2SettingsOrder = ct.h2SettingsOrder.takeIf { it.isNotEmpty() },
                pseudoHeaderOrder = ct.pseudoHeaderOrder.takeIf { it.isNotEmpty() },
                connectionFlow = ct.connectionFlow.toInt(),
                priorityFrames = ct.priorityFrames.map { p ->
                    PriorityFrameJson(p.streamID, PriorityParamJson(p.priorityParam.streamDep, p.priorityParam.exclusive, p.priorityParam.weight))
                }.takeIf { it.isNotEmpty() },
                headerPriority = ct.headerPriority?.let { PriorityParamJson(it.streamDep, it.exclusive, it.weight) },
                certCompressionAlgos = ct.certCompressionAlgos.takeIf { it.isNotEmpty() },
                keyShareCurves = ct.keyShareCurves.takeIf { it.isNotEmpty() },
                supportedSignatureAlgorithms = ct.supportedSignatureAlgorithms.takeIf { it.isNotEmpty() },
                supportedVersions = ct.supportedVersions.takeIf { it.isNotEmpty() },
            )
        },
        sessionId = sessionId,
        followRedirects = followRedirects,
        insecureSkipVerify = insecureSkipVerify,
        isByteResponse = isByteResponse,
        isByteRequest = isByteRequest,
        withoutCookieJar = withoutCookieJar,
        withCustomCookieJar = withCustomCookieJar,
        withRandomTLSExtensionOrder = withRandomTLSExtensionOrder,
        timeoutSeconds = timeoutSeconds,
        timeoutMilliseconds = timeoutMilliseconds,
        proxyUrl = proxyUrl,
        headers = headers.takeIf { it.isNotEmpty() },
        headerOrder = headerOrder.takeIf { it.isNotEmpty() },
        defaultHeaders = defaultHeaders,
        connectHeaders = connectHeaders,
        forceHttp1 = forceHttp1,
        disableHttp3 = disableHttp3,
        withProtocolRacing = withProtocolRacing,
        disableIPV6 = disableIPV6,
        disableIPV4 = disableIPV4,
        localAddress = localAddress,
        serverNameOverwrite = serverNameOverwrite,
        requestHostOverride = requestHostOverride,
        certificatePinningHosts = certificatePinningHosts.takeIf { it.isNotEmpty() },
        catchPanics = catchPanics,
        withDebug = withDebug,
        isRotatingProxy = isRotatingProxy,
    )
}

internal fun RequestPayload.toRequestJson(): String = gson.toJson(this.toRequestInputJson())

internal fun String.parseResponseJson(): ResponseData {
    val j = gson.fromJson(this, ResponseJson::class.java)
        ?: return ResponseData(
            status = 0,
            body = this,
            headers = emptyMap(),
            cookies = emptyMap(),
            target = "",
            usedProtocol = "HTTP/1.1",
        )
    return ResponseData(
        status = j.status,
        body = j.body,
        headers = j.headers ?: emptyMap(),
        cookies = j.cookies ?: emptyMap(),
        target = j.target ?: "",
        sessionId = j.sessionId,
        usedProtocol = j.usedProtocol ?: "HTTP/1.1",
        id = j.id,
    )
}

internal fun ResponseData.toResponseJson(): String = gson.toJson(
    ResponseJson(id = id, status = status, body = body, headers = headers, cookies = cookies, target = target, sessionId = sessionId, usedProtocol = usedProtocol)
)

internal fun DestroySessionPayload.toJson(): String = gson.toJson(mapOf("sessionId" to sessionId))
internal fun GetCookiesFromSessionPayload.toJson(): String = gson.toJson(mapOf("sessionId" to sessionId, "url" to url))

internal data class GetCookiesResponseJson(val id: String, val cookies: List<CookieJson>)
internal fun String.parseGetCookiesResponse(): GetCookiesFromSessionResponse {
    val j = gson.fromJson(this, GetCookiesResponseJson::class.java)
        ?: return GetCookiesFromSessionResponse(id = "", cookies = emptyList())
    return GetCookiesFromSessionResponse(
        id = j.id,
        cookies = j.cookies.map { c -> Cookie(c.name, c.value, c.path, c.domain, c.expires, c.maxAge, c.secure, c.httpOnly) }
    )
}
