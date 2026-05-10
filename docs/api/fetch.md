# fetch

One-shot HTTP request. Creates a temporary [`Session`](./session.md), performs the call, closes the session.

```kotlin
fun fetch(
    url: String,
    method: RequestMethod = RequestMethod.GET,
    options: SessionOptions = SessionOptions(),
    requestOptions: RequestOptions = RequestOptions()
): Response
```

If [`Client.init()`](./client.md) has been called, `fetch` reuses the shared `TlsClient`. Otherwise it constructs a temporary one for the call.

## Quick examples

### GET

```kotlin
import dev.kotlintls.client.fetch

val resp = fetch("https://httpbin.org/get")
println(resp.status)
println(resp.body)
```

### POST with body and headers

```kotlin
import dev.kotlintls.client.fetch
import dev.kotlintls.models.RequestMethod
import dev.kotlintls.models.RequestOptions

val resp = fetch(
    url = "https://httpbin.org/post",
    method = RequestMethod.POST,
    requestOptions = RequestOptions(
        body = """{"key":"value"}""",
        headers = mapOf("Content-Type" to "application/json")
    )
)
```

### Custom TLS profile

```kotlin
import dev.kotlintls.client.fetch
import dev.kotlintls.models.ClientIdentifier
import dev.kotlintls.models.RequestMethod
import dev.kotlintls.models.SessionOptions

val resp = fetch(
    url = "https://tls.peet.ws/api/all",
    method = RequestMethod.GET,
    options = SessionOptions(clientIdentifier = ClientIdentifier.SAFARI_IOS_18_0)
)
```

## When to use what

| You want… | Use |
|---|---|
| One request, throwaway state | `fetch` |
| Many requests, same cookie jar | [`Session`](./session.md) |
| Full control over the Go FFI payload | [`TlsClient`](./tls-client.md) |

## See also

- [Session](./session.md): stateful version
- [Client](./client.md): optional shared singleton
