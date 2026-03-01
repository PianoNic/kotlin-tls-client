package dev.kotlintls

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.util.concurrent.TimeUnit

/**
 * HTTP client with the same API as the Go/Node tls-clients.
 *
 * - Default: uses OkHttp. Requests do not look like a specific browser.
 * - With NativeTlsEngine: uses the Go tls-client library so requests look like a real browser.
 *   Use TlsClient(NativeTlsEngine()) after loading the Go library and JNI bridge; see docs/TLS_FINGERPRINTING.md.
 */
class TlsClient @JvmOverloads constructor(
    private val engine: TlsClientEngine? = null
) {

    private val sessions = ConcurrentHashMap<String, SessionState>()
    private val lock = Any()

    data class SessionState(
        val client: OkHttpClient,
        val cookieJar: MutableCookieJar
    )

    /**
     * Synchronous request. Matches Go/Node request(payload) -> Response.
     * When [engine] is set (e.g. [NativeTlsEngine]), uses it for real JA3; otherwise uses OkHttp.
     */
    fun request(payload: RequestPayload): ResponseData {
        if (engine != null) {
            val json = payload.toRequestJson()
            val responseJson = engine.request(json)
            return responseJson.parseResponseJson()
        }
        val sessionId = payload.sessionId ?: UUID.randomUUID().toString()
        val useSession = payload.sessionId != null

        val state = getOrCreateSession(sessionId, payload)
        val (client, cookieJar) = state

        val timeoutMs = when {
            payload.timeoutMilliseconds > 0 -> payload.timeoutMilliseconds
            else -> payload.timeoutSeconds * 1000
        }

        val url = payload.requestUrl
        val method = payload.requestMethod.name
        val body = payload.requestBody?.takeIf { it.isNotBlank() }
            ?.let { it.toRequestBody(null) }

        val requestBuilder = Request.Builder().url(url).method(method, body)

        // Header order: if headerOrder is set, add headers in that order then remaining
        val headerOrder = payload.headerOrder
        if (headerOrder.isNotEmpty()) {
            headerOrder.forEach { name ->
                payload.headers[name]?.let { requestBuilder.addHeader(name, it) }
            }
            payload.headers.keys.filter { it !in headerOrder }.forEach { name ->
                payload.headers[name]?.let { requestBuilder.addHeader(name, it) }
            }
        } else {
            payload.headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        }

        payload.requestHostOverride?.let { requestBuilder.addHeader("Host", it) }

        // Apply request cookies to jar so they're sent
        if (!payload.withoutCookieJar) {
            payload.requestCookies.forEach { c ->
                url.toHttpUrlOrNull()?.let { u ->
                    cookieJar.add(u, Cookie.Builder().name(c.name).value(c.value).domain(c.domain.ifEmpty { u.host }).path(c.path).build())
                }
            }
        }

        val call = client.newBuilder()
            .callTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .connectTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .build()
            .newCall(requestBuilder.build())

        val resp = try {
            call.execute()
        } catch (e: Exception) {
            return ResponseData(
                status = 0,
                body = e.message ?: e.toString(),
                headers = emptyMap(),
                cookies = emptyMap(),
                target = url,
                sessionId = if (useSession) sessionId else null,
                usedProtocol = "HTTP/1.1"
            )
        }

        val respBody = resp.body?.string() ?: ""
        val respHeaders = resp.headers.toMultimap()
        val respCookies = cookieJar.get(url.toHttpUrlOrNull() ?: return responseFromOkHttp(resp, respBody, respHeaders, emptyMap(), url, if (useSession) sessionId else null))
            .associate { it.name to it.value }

        if (!useSession) {
            removeSession(sessionId)
        }

        return ResponseData(
            status = resp.code,
            body = respBody,
            headers = respHeaders,
            cookies = respCookies,
            target = resp.request.url.toString(),
            sessionId = if (useSession) sessionId else null,
            usedProtocol = resp.protocol.toString(),
            id = UUID.randomUUID().toString()
        )
    }

    private fun responseFromOkHttp(
        resp: okhttp3.Response,
        body: String,
        headers: Map<String, List<String>>,
        cookies: Map<String, String>,
        target: String,
        sessionId: String?
    ): ResponseData {
        return ResponseData(
            status = resp.code,
            body = body,
            headers = headers,
            cookies = cookies,
            target = target,
            sessionId = sessionId,
            usedProtocol = resp.protocol.toString(),
            id = UUID.randomUUID().toString()
        )
    }

    /**
     * Destroy a session (remove from cache, close connections).
     */
    fun destroySession(payload: DestroySessionPayload): DestroySessionResponse {
        if (engine != null) {
            val out = engine.destroySession(payload.toJson())
            return com.google.gson.Gson().fromJson(out, DestroySessionResponse::class.java)
        }
        removeSession(payload.sessionId)
        return DestroySessionResponse(id = UUID.randomUUID().toString(), success = true)
    }

    /**
     * Get cookies for a URL from a session.
     */
    fun getCookiesFromSession(payload: GetCookiesFromSessionPayload): GetCookiesFromSessionResponse {
        if (engine != null) {
            val out = engine.getCookiesFromSession(payload.toJson())
            return out.parseGetCookiesResponse()
        }
        val state = sessions[payload.sessionId] ?: return GetCookiesFromSessionResponse(
            id = UUID.randomUUID().toString(),
            cookies = emptyList()
        )
        val url = payload.url.toHttpUrlOrNull() ?: return GetCookiesFromSessionResponse(
            id = UUID.randomUUID().toString(),
            cookies = emptyList()
        )
        val cookies = state.cookieJar.get(url).map { c ->
            dev.kotlintls.Cookie(
                name = c.name,
                value = c.value,
                path = c.path,
                domain = c.domain,
                expires = c.expiresAt,
                maxAge = -1,
                secure = c.secure,
                httpOnly = c.httpOnly
            )
        }
        return GetCookiesFromSessionResponse(id = UUID.randomUUID().toString(), cookies = cookies)
    }

    /**
     * Destroy all sessions (clear cache).
     */
    fun destroyAll(): DestroySessionResponse {
        if (engine != null) {
            val out = engine.destroyAll()
            return com.google.gson.Gson().fromJson(out, DestroySessionResponse::class.java)
        }
        synchronized(lock) {
            sessions.keys.toList().forEach { removeSession(it) }
        }
        return DestroySessionResponse(id = UUID.randomUUID().toString(), success = true)
    }

    private fun removeSession(sessionId: String) {
        val state = sessions.remove(sessionId) ?: return
        state.client.dispatcher.executorService.shutdown()
        state.client.connectionPool.evictAll()
    }

    private fun getOrCreateSession(sessionId: String, payload: RequestPayload): SessionState {
        if (payload.sessionId != null) {
            sessions[sessionId]?.let { return it }
        }
        return synchronized(lock) {
            sessions.getOrPut(sessionId) {
                val cookieJar = MutableCookieJar()
                val timeoutMs = when {
                    payload.timeoutMilliseconds > 0 -> payload.timeoutMilliseconds.toLong()
                    else -> payload.timeoutSeconds * 1000L
                }
                val builder = OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .followRedirects(payload.followRedirects)
                    .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)

                if (payload.insecureSkipVerify) {
                    val trustAll = object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {}
                        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                    val sslContext = SSLContext.getInstance("TLS").apply {
                        init(null, arrayOf<TrustManager>(trustAll), null)
                    }
                    builder.sslSocketFactory(sslContext.socketFactory, trustAll)
                    builder.hostnameVerifier { _, _ -> true }
                }

                payload.proxyUrl?.takeIf { it.isNotBlank() }?.let { proxyUrl ->
                    val url = java.net.URI.create(proxyUrl)
                    val host = url.host ?: return@let
                    val port = url.port.takeIf { it > 0 } ?: when (url.scheme) {
                        "https" -> 443
                        else -> 80
                    }
                    builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
                }

                payload.transportOptions?.let { opt ->
                    if (opt.disableKeepAlives) builder.retryOnConnectionFailure(false)
                    opt.idleConnTimeout?.let { builder.connectionPool(okhttp3.ConnectionPool(10, it, java.util.concurrent.TimeUnit.MILLISECONDS)) }
                }

                SessionState(client = builder.build(), cookieJar = cookieJar)
            }
        }
    }
}

/**
 * Mutable cookie jar that stores cookies in memory (matches Go/Node cookie jar behavior).
 */
class MutableCookieJar : CookieJar {
    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

    private fun key(url: okhttp3.HttpUrl): String = url.host

    override fun loadForRequest(url: okhttp3.HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return store[key(url)]?.filter { c -> c.expiresAt == 0L || c.expiresAt > now } ?: emptyList()
    }

    override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<Cookie>) {
        store.getOrPut(key(url)) { mutableListOf() }.let { list ->
            cookies.forEach { c ->
                list.removeAll { it.name == c.name && it.domain == c.domain && it.path == c.path }
                list.add(c)
            }
        }
    }

    fun add(url: okhttp3.HttpUrl, cookie: Cookie) {
        store.getOrPut(key(url)) { mutableListOf() }.add(cookie)
    }

    fun get(url: okhttp3.HttpUrl): List<Cookie> = loadForRequest(url)
}
