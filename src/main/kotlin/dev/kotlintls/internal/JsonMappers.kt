package dev.kotlintls.internal

import com.google.gson.Gson
import dev.kotlintls.models.Cookie
import dev.kotlintls.models.DestroySessionPayload
import dev.kotlintls.models.DestroySessionResponse
import dev.kotlintls.models.GetCookiesFromSessionPayload
import dev.kotlintls.models.GetCookiesFromSessionResponse
import dev.kotlintls.models.RequestPayload
import dev.kotlintls.models.ResponseData

private val gson = Gson()

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
    val j = try {
        gson.fromJson(this, ResponseJson::class.java)
    } catch (_: Exception) {
        null
    } ?: return ResponseData(
        status = 0,
        body = this,
        headers = emptyMap(),
        cookies = emptyMap(),
        target = "",
        usedProtocol = "HTTP/1.1",
    )
    return ResponseData(
        status = j.status ?: 0,
        body = j.body ?: "",
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

internal fun String.parseDestroySessionResponse(): DestroySessionResponse =
    gson.fromJson(this, DestroySessionResponse::class.java)
        ?: DestroySessionResponse(id = "", success = false)

internal fun String.parseGetCookiesResponse(): GetCookiesFromSessionResponse {
    val j = gson.fromJson(this, GetCookiesResponseJson::class.java)
        ?: return GetCookiesFromSessionResponse(id = "", cookies = emptyList())
    return GetCookiesFromSessionResponse(
        id = j.id ?: "",
        cookies = j.cookies?.map { c -> Cookie(c.name, c.value, c.path, c.domain, c.expires, c.maxAge, c.secure, c.httpOnly) } ?: emptyList()
    )
}
