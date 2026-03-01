package dev.kotlintls

/**
 * Kotlin TLS Client — API parity with Go tls-client and Node node-tls-client.
 *
 * Features (exact feature set):
 * - HTTP/1.1 (HTTP/2 via OkHttp when supported)
 * - Session-based requests: Session with get/post/put/delete/patch/head/options, close(), cookies()
 * - One-shot fetch(url, method, options)
 * - request(payload) / destroySession / getCookiesFromSession
 * - TLS client identifiers (browser profiles: chrome_*, firefox_*, safari_*, etc.)
 * - Custom TLS (JA3 string, h2 settings, pseudo header order) — API present; full JA3 on JVM requires native
 * - Proxy (HTTP)
 * - Cookie jar (per-session), getCookiesFromSession
 * - Timeout (seconds/milliseconds), followRedirects, insecureSkipVerify
 * - Header order, default headers, connect headers
 * - Transport options (keep-alive, compression, etc.)
 * - Certificate pinning (API), server name overwrite, local address
 * - forceHttp1, disableHttp3, withProtocolRacing (config only; OkHttp negotiates)
 * - disableIPV4 / disableIPV6 (config)
 */
object TlsClientKt
