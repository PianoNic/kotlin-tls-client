package dev.kotlintls

import java.util.UUID

/**
 * Session options matching Node SessionOptions / Go client config.
 */
data class SessionOptions(
    val sessionId: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val connectHeaders: Map<String, List<String>> = emptyMap(),
    val proxy: String? = null,
    val isRotatingProxy: Boolean = false,
    val clientIdentifier: ClientIdentifier = ClientIdentifier.DEFAULT,
    val ja3String: String? = null,
    val customTlsClient: CustomTlsClient? = null,
    val h2Settings: Map<String, UInt> = emptyMap(),
    val h2SettingsOrder: List<String> = emptyList(),
    val pseudoHeaderOrder: List<String> = emptyList(),
    val connectionFlow: UInt = 0u,
    val priorityFrames: List<PriorityFrame> = emptyList(),
    val headerPriority: PriorityParam? = null,
    val certCompressionAlgo: String = "zlib",
    val supportedVersions: List<String> = emptyList(),
    val supportedSignatureAlgorithms: List<String> = emptyList(),
    val keyShareCurves: List<String> = emptyList(),
    val alpnProtocols: List<String> = emptyList(),
    val alpsProtocols: List<String> = emptyList(),
    val serverNameOverwrite: String? = null,
    val streamOutputBlockSize: Int? = null,
    val streamOutputEOFSymbol: String? = null,
    val streamOutputPath: String? = null,
    val localAddress: String? = null,
    val transportOptions: TransportOptions? = null,
    val headerOrder: List<String> = emptyList(),
    val randomTlsExtensionOrder: Boolean = false,
    val forceHttp1: Boolean = false,
    val debug: Boolean = false,
    val insecureSkipVerify: Boolean = false,
    val timeout: Int = 0,
    val disableIPV4: Boolean = false,
    val disableIPV6: Boolean = false,
    val followRedirects: Boolean = false
)

/**
 * Request options for a single request (method-specific), matching Node GetRequestOptions, PostRequestOptions, etc.
 */
data class RequestOptions(
    val headers: Map<String, String> = emptyMap(),
    val connectHeaders: Map<String, List<String>>? = null,
    val headerOrder: List<String> = emptyList(),
    val followRedirects: Boolean? = null,
    val proxy: String? = null,
    val isRotatingProxy: Boolean? = null,
    val cookies: Map<String, String> = emptyMap(),
    val byteResponse: Boolean = false,
    val hostOverride: String? = null,
    val body: String? = null
)

/**
 * Session: holds sessionId and config, provides get/post/put/delete/patch/head/options, close(), cookies().
 * Matches Node Session API.
 */
class Session(
    private val tlsClient: TlsClient,
    private val config: SessionOptions = SessionOptions()
) {
    val sessionId: String = config.sessionId ?: UUID.randomUUID().toString()

    private fun basePayload(
        method: RequestMethod,
        url: String,
        options: RequestOptions = RequestOptions()
    ): RequestPayload {
        val headers = options.headers.ifEmpty { config.headers }.ifEmpty { defaultHeaders() }
        val requestCookies = options.cookies.map { (name, value) -> Cookie(name, value) }
        val (tlsId, customTls) = when {
            config.customTlsClient != null -> null to config.customTlsClient
            config.ja3String != null -> null to (config.customTlsClient ?: CustomTlsClient(ja3String = config.ja3String))
            else -> config.clientIdentifier.value to null
        }
        return RequestPayload(
            requestUrl = url,
            requestMethod = method,
            requestBody = options.body,
            requestCookies = requestCookies,
            tlsClientIdentifier = tlsId,
            customTlsClient = customTls,
            sessionId = sessionId,
            followRedirects = options.followRedirects ?: config.followRedirects,
            insecureSkipVerify = config.insecureSkipVerify,
            isByteResponse = options.byteResponse,
            proxyUrl = options.proxy ?: config.proxy,
            headers = headers,
            headerOrder = options.headerOrder.ifEmpty { config.headerOrder },
            connectHeaders = options.connectHeaders ?: config.connectHeaders,
            forceHttp1 = config.forceHttp1,
            withRandomTLSExtensionOrder = config.randomTlsExtensionOrder,
            timeoutSeconds = if (config.timeout > 0) config.timeout / 1000 else 30,
            timeoutMilliseconds = if (config.timeout > 0) config.timeout else 0,
            disableIPV4 = config.disableIPV4,
            disableIPV6 = config.disableIPV6,
            serverNameOverwrite = config.serverNameOverwrite,
            requestHostOverride = options.hostOverride,
            transportOptions = config.transportOptions,
            isRotatingProxy = options.isRotatingProxy ?: config.isRotatingProxy,
            streamOutputPath = config.streamOutputPath,
            streamOutputBlockSize = config.streamOutputBlockSize,
            streamOutputEOFSymbol = config.streamOutputEOFSymbol
        )
    }

    private fun defaultHeaders(): Map<String, String> = mapOf(
        "User-Agent" to "tls-client/1.0",
        "Accept-Encoding" to "gzip, deflate, br",
        "Accept" to "*/*",
        "Connection" to "keep-alive"
    )

    fun get(url: String, options: RequestOptions = RequestOptions()) =
        execute(RequestMethod.GET, url, options)

    fun post(url: String, options: RequestOptions = RequestOptions()) =
        execute(RequestMethod.POST, url, options)

    fun put(url: String, options: RequestOptions = RequestOptions()) =
        execute(RequestMethod.PUT, url, options)

    fun delete(url: String, options: RequestOptions = RequestOptions()) =
        execute(RequestMethod.DELETE, url, options)

    fun patch(url: String, options: RequestOptions = RequestOptions()) =
        execute(RequestMethod.PATCH, url, options)

    fun head(url: String, options: RequestOptions = RequestOptions()) =
        execute(RequestMethod.HEAD, url, options)

    fun options(url: String, options: RequestOptions = RequestOptions()) =
        execute(RequestMethod.OPTIONS, url, options)

    private fun execute(method: RequestMethod, url: String, options: RequestOptions): Response {
        val payload = basePayload(method, url, options)
        val data = tlsClient.request(payload)
        return Response(data)
    }

    /**
     * Close the session (destroy on server side).
     */
    fun close(): DestroySessionResponse =
        tlsClient.destroySession(DestroySessionPayload(sessionId))

    /**
     * Get cookies for a URL from this session.
     */
    fun cookies(url: String): List<Cookie> =
        tlsClient.getCookiesFromSession(GetCookiesFromSessionPayload(sessionId, url)).cookies
}

/**
 * Response wrapper matching Node Response (ok, status, headers, body, cookies, url, text(), json()).
 */
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
