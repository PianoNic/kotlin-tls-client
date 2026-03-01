package dev.kotlintls

/**
 * HTTP methods matching the Go/Node TLS client API.
 */
enum class RequestMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
}

/**
 * Response data returned from request(), matching Go Response / Node TlsResponse.
 */
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

/**
 * Cookie structure matching Go/Node cookie representation.
 */
data class Cookie(
    val name: String,
    val value: String,
    val path: String = "/",
    val domain: String = "",
    val expires: Long = 0L,
    val maxAge: Int = -1,
    val secure: Boolean = false,
    val httpOnly: Boolean = false
)

/**
 * Transport options for the underlying HTTP client (matches Go TransportOptions).
 */
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

/**
 * HTTP/2 priority parameter (matches Go PriorityParam).
 */
data class PriorityParam(
    val streamDep: Int,
    val exclusive: Boolean,
    val weight: Int
)

/**
 * HTTP/2 priority frame (matches Go PriorityFrames).
 */
data class PriorityFrame(
    val streamID: Int,
    val priorityParam: PriorityParam
)

/**
 * Custom TLS client specification (JA3, h2 settings, etc.) for API parity with Go/Node.
 * Full JA3 fingerprinting on JVM would require native/Conscrypt integration.
 */
data class CustomTlsClient(
    val ja3String: String,
    val h2Settings: Map<String, UInt> = emptyMap(),
    val h2SettingsOrder: List<String> = emptyList(),
    val h3Settings: Map<String, ULong>? = null,
    val h3SettingsOrder: List<String>? = null,
    val h3PseudoHeaderOrder: List<String>? = null,
    val headerPriority: PriorityParam? = null,
    val certCompressionAlgos: List<String> = emptyList(),
    val keyShareCurves: List<String> = emptyList(),
    val supportedSignatureAlgorithms: List<String> = emptyList(),
    val supportedVersions: List<String> = emptyList(),
    val pseudoHeaderOrder: List<String> = emptyList(),
    val priorityFrames: List<PriorityFrame> = emptyList(),
    val connectionFlow: UInt = 0u,
    val streamId: UInt = 0u,
    val h3PriorityParam: UInt = 0u,
    val h3SendGreaseFrames: Boolean = false,
    val allowHttp: Boolean = false,
    val alpnProtocols: List<String> = emptyList(),
    val alpsProtocols: List<String> = emptyList()
)

/**
 * Request payload matching Go RequestInput / Node Payload.
 * Used for request() and internally by Session.
 */
data class RequestPayload(
    val requestUrl: String,
    val requestMethod: RequestMethod = RequestMethod.GET,
    val requestBody: String? = null,
    val requestCookies: List<Cookie> = emptyList(),
    val tlsClientIdentifier: String? = null,
    val customTlsClient: CustomTlsClient? = null,
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
    val headers: Map<String, String> = emptyMap(),
    val headerOrder: List<String> = emptyList(),
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
    val certificatePinningHosts: Map<String, List<String>> = emptyMap(),
    val catchPanics: Boolean = false,
    val withDebug: Boolean = false,
    val isRotatingProxy: Boolean = false,
    val streamOutputPath: String? = null,
    val streamOutputBlockSize: Int? = null,
    val streamOutputEOFSymbol: String? = null,
    val transportOptions: TransportOptions? = null
)

/**
 * Session destroy payload / response.
 */
data class DestroySessionPayload(val sessionId: String)
data class DestroySessionResponse(val id: String, val success: Boolean)

/**
 * Get cookies from session payload / response.
 */
data class GetCookiesFromSessionPayload(val sessionId: String, val url: String)
data class GetCookiesFromSessionResponse(val id: String, val cookies: List<Cookie>)
