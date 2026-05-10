package dev.kotlintls

import java.util.UUID

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
            isByteRequest = options.byteRequest,
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

    fun close(): DestroySessionResponse =
        tlsClient.destroySession(DestroySessionPayload(sessionId))

    fun cookies(url: String): List<Cookie> =
        tlsClient.getCookiesFromSession(GetCookiesFromSessionPayload(sessionId, url)).cookies
}
