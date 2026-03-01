# TlsClient

The core class. Matches the Go `request` / `destroySession` / `getCookiesFromSession` / `destroyAll` FFI API.

## Constructor

```kotlin
// Default: uses OkHttp (no browser fingerprint)
val client = TlsClient()

// With native engine: uses Go tls-client (real browser fingerprint)
val client = TlsClient(NativeTlsEngine())
```

## Methods

### `request(payload: RequestPayload): ResponseData`

Performs an HTTP request.

```kotlin
val data = client.request(RequestPayload(
    requestUrl = "https://httpbin.org/get",
    requestMethod = RequestMethod.GET,
    sessionId = "my-session",
    tlsClientIdentifier = ClientIdentifier.CHROME_133.value,
    headers = mapOf("Accept" to "application/json"),
    followRedirects = true,
    timeoutSeconds = 30
))
println(data.status)   // 200
println(data.body)
println(data.ok)       // true
```

### `destroySession(payload: DestroySessionPayload): DestroySessionResponse`

Removes a session and closes its connections.

```kotlin
val result = client.destroySession(DestroySessionPayload("my-session"))
println(result.success)  // true
```

### `getCookiesFromSession(payload: GetCookiesFromSessionPayload): GetCookiesFromSessionResponse`

Returns all cookies stored for a URL in a given session.

```kotlin
val result = client.getCookiesFromSession(
    GetCookiesFromSessionPayload("my-session", "https://httpbin.org")
)
result.cookies.forEach { println("${it.name}=${it.value}") }
```

### `destroyAll(): DestroySessionResponse`

Destroys all sessions.

```kotlin
client.destroyAll()
```

## Session lifecycle

Sessions are created automatically on the first request that includes a `sessionId`. They persist until you call `destroySession` or `destroyAll`.

```kotlin
// First request: creates session "s1"
client.request(RequestPayload(requestUrl = "https://example.com", sessionId = "s1"))

// Second request: reuses session "s1" (same cookies, same connection pool)
client.request(RequestPayload(requestUrl = "https://example.com/page2", sessionId = "s1"))

// Done: clean up
client.destroySession(DestroySessionPayload("s1"))
```

If no `sessionId` is set, a random one is created and destroyed after the request.

## See also

- [Session](./session.md) – Higher-level wrapper around TlsClient
- [Types](./types.md) – RequestPayload, ResponseData, and all other types
- [NativeTlsEngine](./native-engine.md) – Real browser fingerprint
