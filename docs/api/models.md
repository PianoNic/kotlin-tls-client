# Models

All public data classes and enums live under `dev.kotlintls.models`.

```kotlin
import dev.kotlintls.models.*
```

## Request

### RequestMethod

```kotlin
enum class RequestMethod { GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS }
```

### RequestPayload

The full payload accepted by [`TlsClient.request`](./tls-client.md#requestpayload-requestpayload-responsedata). Most users build this through a [`Session`](./session.md), but you can construct it directly for one-off calls.

Frequently set fields:

| Field | Type | Default | Notes |
|---|---|---|---|
| `requestUrl` | `String` | (required) | Target URL |
| `requestMethod` | `RequestMethod` | `GET` | |
| `requestBody` | `String?` | `null` | Body as a string |
| `requestCookies` | `List<Cookie>` | empty | Per-request cookies |
| `tlsClientIdentifier` | `String?` | `null` | e.g. `"chrome_133"` |
| `customTlsClient` | `CustomTlsClient?` | `null` | Full custom TLS config |
| `sessionId` | `String?` | `null` | Reuse cookie jar across calls |
| `followRedirects` | `Boolean` | `false` | |
| `headers` | `Map<String, String>` | empty | |
| `headerOrder` | `List<String>` | empty | |
| `proxyUrl` | `String?` | `null` | |
| `timeoutSeconds` / `timeoutMilliseconds` | `Int` | `30` / `0` | |
| `insecureSkipVerify` | `Boolean` | `false` | |
| `forceHttp1` | `Boolean` | `false` | |
| `serverNameOverwrite` | `String?` | `null` | Custom SNI |
| `transportOptions` | `TransportOptions?` | `null` | |

See `RequestPayload.kt` for the complete list (~35 fields, all matching the Go FFI).

### RequestOptions

Per-call overrides used by [`Session`](./session.md). Subset of `RequestPayload` for ergonomics:

```kotlin
data class RequestOptions(
    val headers: Map<String, String> = emptyMap(),
    val connectHeaders: Map<String, List<String>>? = null,
    val headerOrder: List<String> = emptyList(),
    val followRedirects: Boolean? = null,
    val proxy: String? = null,
    val isRotatingProxy: Boolean? = null,
    val cookies: Map<String, String> = emptyMap(),
    val byteResponse: Boolean = false,
    val byteRequest: Boolean = false,
    val hostOverride: String? = null,
    val body: String? = null
)
```

## Response

### ResponseData

Raw response from the Go engine.

```kotlin
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
```

### Response

Wrapper returned by [`Session`](./session.md) methods. Same fields as `ResponseData` exposed as properties: `ok`, `status`, `headers`, `body`, `cookies`, `url`, `sessionId`, `usedProtocol`. Plus `text(): String`.

## Cookies

```kotlin
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
```

## Session config

### SessionOptions

The defaults that apply to every request in a `Session`. Documented in detail on the [Session page](./session.md#sessionoptions).

### DestroySessionPayload / DestroySessionResponse

```kotlin
data class DestroySessionPayload(val sessionId: String)
data class DestroySessionResponse(val id: String, val success: Boolean)
```

### GetCookiesFromSessionPayload / GetCookiesFromSessionResponse

```kotlin
data class GetCookiesFromSessionPayload(val sessionId: String, val url: String)
data class GetCookiesFromSessionResponse(val id: String, val cookies: List<Cookie>)
```

## TLS

### ClientIdentifier

Enum of ~70 browser TLS profile presets. The `value` is the string the Go library expects.

```kotlin
ClientIdentifier.CHROME_133.value   // "chrome_133"
ClientIdentifier.DEFAULT            // CHROME_133
ClientIdentifier.fromString("firefox_135")  // ClientIdentifier.FIREFOX_135
```

Categories: Chrome (103–146), Firefox (102–147), Safari (15–18, iOS variants), Opera (89–91), and mobile/custom profiles (Zalando, Nike, Cloudscraper, OkHttp4 Android, MMS iOS, Mesh iOS/Android, Confirmed iOS/Android).

### CustomTlsClient

Full TLS spec. Use this when you need more control than `ja3String` alone — HTTP/2 settings, pseudo-header order, priority frames, supported versions, etc.

### TransportOptions

Connection-pool tuning passed straight through to Go.

```kotlin
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
```

### PriorityParam / PriorityFrame

HTTP/2 priority frames, used inside `CustomTlsClient`.

## See also

- [TlsClient](./tls-client.md)
- [Session](./session.md)
- [TLS Fingerprinting](../tls-fingerprinting.md) — What `ClientIdentifier` and `CustomTlsClient` actually do
