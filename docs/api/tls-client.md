# TlsClient

The low-level entrypoint. Sends a `RequestPayload` through a `TlsClientEngine` and returns a `ResponseData`. Most users go through [`Session`](./session.md) or [`fetch`](./fetch.md) instead — use `TlsClient` directly when you need full control over the payload.

## Setup

The default constructor uses [`NativeTlsEngine`](./native-engine.md), which loads the bundled Go TLS library via JNA.

```kotlin
import dev.kotlintls.TlsClient

val client = TlsClient()
```

You can pass a custom engine for tests or alternative transports:

```kotlin
import dev.kotlintls.engine.TlsClientEngine

class FakeEngine : TlsClientEngine { /* ... */ }
val client = TlsClient(FakeEngine())
```

## Methods

### request(payload: RequestPayload): ResponseData

Sends a request and returns the parsed response. Not a suspend function.

```kotlin
import dev.kotlintls.models.RequestMethod
import dev.kotlintls.models.RequestPayload

val data = client.request(RequestPayload(
    requestUrl = "https://httpbin.org/get",
    requestMethod = RequestMethod.GET
))
println(data.status)
println(data.body)
```

### destroySession(payload: DestroySessionPayload): DestroySessionResponse

Destroys a session on the Go-side cookie jar.

```kotlin
import dev.kotlintls.models.DestroySessionPayload

client.destroySession(DestroySessionPayload(sessionId = "my-session"))
```

### getCookiesFromSession(payload: GetCookiesFromSessionPayload): GetCookiesFromSessionResponse

Returns cookies the Go cookie jar holds for a given URL in a session.

```kotlin
import dev.kotlintls.models.GetCookiesFromSessionPayload

val result = client.getCookiesFromSession(
    GetCookiesFromSessionPayload(sessionId = "my-session", url = "https://example.com")
)
result.cookies.forEach { println("${it.name}=${it.value}") }
```

### destroyAll(): DestroySessionResponse

Destroys every session in the Go cookie jar. Call this on shutdown if you don't track session IDs yourself.

```kotlin
client.destroyAll()
```

## Example

```kotlin
import dev.kotlintls.TlsClient
import dev.kotlintls.models.ClientIdentifier
import dev.kotlintls.models.DestroySessionPayload
import dev.kotlintls.models.RequestMethod
import dev.kotlintls.models.RequestPayload

fun main() {
    val client = TlsClient()
    val data = client.request(RequestPayload(
        requestUrl = "https://httpbin.org/post",
        requestMethod = RequestMethod.POST,
        requestBody = """{"hello":"world"}""",
        tlsClientIdentifier = ClientIdentifier.CHROME_133.value,
        sessionId = "demo",
        headers = mapOf("Content-Type" to "application/json")
    ))
    println(data.status)
    client.destroySession(DestroySessionPayload("demo"))
}
```

## See also

- [Session](./session.md) — Higher-level wrapper for repeated requests
- [Models](./models.md) — `RequestPayload`, `ResponseData`, and friends
- [NativeTlsEngine](./native-engine.md) — The default transport
