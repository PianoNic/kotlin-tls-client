# fetch

One-shot request. Creates a session, performs the request, closes the session. No state is kept.

Matches the Node `fetch(url, options)` API.

## Signature

```kotlin
fun fetch(
    url: String,
    method: RequestMethod = RequestMethod.GET,
    options: SessionOptions = SessionOptions(),
    requestOptions: RequestOptions = RequestOptions()
): Response
```

## Usage

```kotlin
import dev.kotlintls.*

// Simple GET
val resp = fetch("https://httpbin.org/get")
println(resp.status)  // 200

// POST
val resp = fetch(
    url = "https://httpbin.org/post",
    method = RequestMethod.POST,
    requestOptions = RequestOptions(
        headers = mapOf("Content-Type" to "application/json"),
        body = """{"key":"value"}"""
    )
)

// With TLS profile and proxy
val resp = fetch(
    url = "https://httpbin.org/get",
    options = SessionOptions(
        clientIdentifier = ClientIdentifier.FIREFOX_133,
        proxy = "http://proxy.example.com:8080",
        followRedirects = true
    )
)
println(resp.ok)
```

## Notes

- If `Client.init()` was called, `fetch` reuses the shared `TlsClient`. Otherwise it creates a temporary one.
- The session is always closed after the request, so cookies are not carried over between calls.
- For persistent cookies across requests, use `Session` instead.

## See also

- [Session](./session.md) – Persistent session with cookie jar
- [Client](./client.md) – Shared client singleton
