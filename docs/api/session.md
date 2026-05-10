# Session

Stateful wrapper around [`TlsClient`](./tls-client.md). Holds a `sessionId`, a default `SessionOptions`, and exposes one method per HTTP verb. Cookies persist on the Go side until you call `close()`.

## Setup

A `Session` needs a `TlsClient`. You can build one yourself or reuse the singleton.

```kotlin
import dev.kotlintls.client.Client
import dev.kotlintls.models.ClientIdentifier
import dev.kotlintls.models.SessionOptions
import dev.kotlintls.session.Session

Client.init()
val session = Session(Client.getInstance(), SessionOptions(
    clientIdentifier = ClientIdentifier.CHROME_133,
    followRedirects = true,
    timeout = 30_000
))
```

## Methods

### get / post / put / delete / patch / head / options

All seven verbs share the same signature: `(url: String, options: RequestOptions = RequestOptions()): Response`.

```kotlin
val r1 = session.get("https://httpbin.org/get")

val r2 = session.post("https://httpbin.org/post", RequestOptions(
    body = """{"key":"value"}""",
    headers = mapOf("Content-Type" to "application/json")
))
```

### close(): DestroySessionResponse

Destroys the session on the Go side. Call this when you're done.

```kotlin
session.close()
```

### cookies(url: String): List\<Cookie\>

Returns cookies the Go cookie jar holds for the given URL in this session.

```kotlin
val jar = session.cookies("https://httpbin.org")
jar.forEach { println("${it.name}=${it.value}") }
```

## Properties

- **`sessionId: String`** — The session ID. Auto-generated UUID unless you pass `SessionOptions(sessionId = ...)`.

## SessionOptions

Frequently used fields:

| Field | Type | Default | Description |
|---|---|---|---|
| `sessionId` | `String?` | `null` | Provide a fixed ID instead of a random UUID |
| `clientIdentifier` | `ClientIdentifier` | `CHROME_133` | TLS profile preset |
| `ja3String` | `String?` | `null` | Custom JA3 string (overrides `clientIdentifier`) |
| `customTlsClient` | `CustomTlsClient?` | `null` | Full custom TLS config (overrides both above) |
| `headers` | `Map<String, String>` | `emptyMap()` | Default headers for every request in the session |
| `headerOrder` | `List<String>` | `emptyList()` | Order of headers on the wire |
| `proxy` | `String?` | `null` | HTTP proxy URL (`http://user:pass@host:port`) |
| `followRedirects` | `Boolean` | `false` | Follow 3xx automatically |
| `timeout` | `Int` | `0` | Total timeout in **milliseconds**; `0` falls back to a 30s default |
| `insecureSkipVerify` | `Boolean` | `false` | Skip TLS certificate verification |
| `forceHttp1` | `Boolean` | `false` | Force HTTP/1.1 |
| `disableIPV4` / `disableIPV6` | `Boolean` | `false` | DNS family filtering |
| `serverNameOverwrite` | `String?` | `null` | Custom SNI |
| `transportOptions` | `TransportOptions?` | `null` | Connection pool tuning |

See [Models](./models.md#sessionoptions) for the complete list.

## RequestOptions (per-call overrides)

Per-call options override session config:

| Field | Type | Description |
|---|---|---|
| `headers` | `Map<String, String>` | Replaces session headers for this call |
| `body` | `String?` | Request body |
| `cookies` | `Map<String, String>` | Cookies to send only with this request |
| `followRedirects` | `Boolean?` | Override session setting |
| `proxy` | `String?` | Override session proxy |
| `hostOverride` | `String?` | Override the `Host` header |
| `byteRequest` / `byteResponse` | `Boolean` | Treat body/response as base64 bytes |

## Example

```kotlin
import dev.kotlintls.client.Client
import dev.kotlintls.models.ClientIdentifier
import dev.kotlintls.models.RequestOptions
import dev.kotlintls.models.SessionOptions
import dev.kotlintls.session.Session

fun main() {
    Client.init()

    val session = Session(Client.getInstance(), SessionOptions(
        clientIdentifier = ClientIdentifier.FIREFOX_135,
        followRedirects = true,
        timeout = 15_000
    ))

    // Login
    session.post("https://example.com/login", RequestOptions(
        body = "user=alice&pass=secret",
        headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")
    ))

    // Authenticated call — cookies from /login are reused automatically
    val resp = session.get("https://example.com/account")
    println(resp.status)
    println(resp.body)

    session.close()
    Client.destroy()
}
```

## See also

- [TlsClient](./tls-client.md) — The underlying client
- [fetch](./fetch.md) — One-shot version when you don't need a session
- [Models](./models.md) — `SessionOptions`, `RequestOptions`, `Response`, `Cookie`
