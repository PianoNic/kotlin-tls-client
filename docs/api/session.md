# Session

High-level wrapper around `TlsClient`. Matches the Node `Session` API.

## Constructor

```kotlin
val session = Session(
    tlsClient = TlsClient(),        // or Client.getInstance()
    config = SessionOptions(        // optional
        clientIdentifier = ClientIdentifier.CHROME_133,
        followRedirects = true,
        timeout = 30_000
    )
)
```

`sessionId` is generated automatically unless you set it in `SessionOptions`.

## Methods

### HTTP methods

All methods take a URL and optional `RequestOptions`. They return a `Response`.

```kotlin
session.get(url, options)
session.post(url, options)
session.put(url, options)
session.delete(url, options)
session.patch(url, options)
session.head(url, options)
session.options(url, options)
```

**Example:**

```kotlin
// GET
val resp = session.get("https://httpbin.org/get")
println(resp.status)  // 200
println(resp.ok)      // true

// POST with body
val resp = session.post("https://httpbin.org/post", RequestOptions(
    headers = mapOf("Content-Type" to "application/json"),
    body = """{"hello":"world"}"""
))

// With per-request cookies
val resp = session.get("https://httpbin.org/cookies", RequestOptions(
    cookies = mapOf("session_id" to "abc123")
))
```

### `close(): DestroySessionResponse`

Destroys the session on the underlying client.

```kotlin
session.close()
```

### `cookies(url: String): List<Cookie>`

Returns cookies stored for a URL in this session.

```kotlin
val cookies = session.cookies("https://httpbin.org")
cookies.forEach { println("${it.name}=${it.value}") }
```

## SessionOptions

All fields are optional.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `sessionId` | `String?` | random UUID | Session identifier |
| `clientIdentifier` | `ClientIdentifier` | `CHROME_133` | TLS profile |
| `headers` | `Map<String,String>` | default browser headers | Default headers for all requests |
| `headerOrder` | `List<String>` | empty | Header order |
| `followRedirects` | `Boolean` | `false` | Follow HTTP redirects |
| `insecureSkipVerify` | `Boolean` | `false` | Skip TLS verification |
| `timeout` | `Int` | `0` (30s) | Timeout in milliseconds |
| `proxy` | `String?` | none | HTTP proxy URL, e.g. `http://host:8080` |
| `ja3String` | `String?` | none | Custom JA3 string |
| `customTlsClient` | `CustomTlsClient?` | none | Full custom TLS config |
| `forceHttp1` | `Boolean` | `false` | Force HTTP/1.1 |
| `randomTlsExtensionOrder` | `Boolean` | `false` | Randomize TLS extension order |
| `transportOptions` | `TransportOptions?` | none | Keep-alive, compression, buffer sizes |
| `disableIPV4` / `disableIPV6` | `Boolean` | `false` | Disable address family |

## RequestOptions

Per-request options. Override the session-level config.

| Field | Type | Description |
|-------|------|-------------|
| `headers` | `Map<String,String>` | Override session headers |
| `headerOrder` | `List<String>` | Override header order |
| `body` | `String?` | Request body |
| `cookies` | `Map<String,String>` | Additional cookies to send |
| `followRedirects` | `Boolean?` | Override session redirect setting |
| `proxy` | `String?` | Override session proxy |
| `byteResponse` | `Boolean` | Request raw bytes |
| `hostOverride` | `String?` | Override Host header |

## Response

```kotlin
resp.ok          // Boolean: status in 200..299
resp.status      // Int: HTTP status code
resp.body        // String: response body
resp.text()      // same as body
resp.headers     // Map<String, List<String>>
resp.cookies     // Map<String, String>
resp.url         // String: final URL (after redirects)
resp.usedProtocol  // "http/1.1" or "h2"
resp.sessionId   // String?
```

## Example

```kotlin
import dev.kotlintls.*

fun main() {
    Client.init()
    val session = Session(Client.getInstance(), SessionOptions(
        clientIdentifier = ClientIdentifier.CHROME_133,
        followRedirects = true,
        timeout = 30_000
    ))

    // Make requests; cookies are kept between calls
    session.get("https://httpbin.org/cookies/set/token/abc")
    val resp = session.get("https://httpbin.org/cookies")
    println(resp.body)  // shows {"cookies": {"token": "abc"}}

    println(session.cookies("https://httpbin.org"))
    session.close()
    Client.destroy()
}
```

## See also

- [fetch](./fetch.md) – One-shot request without managing sessions
- [TlsClient](./tls-client.md) – Low-level API
- [Types](./types.md) – All types
