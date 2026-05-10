# Client

Process-wide singleton holding a default [`TlsClient`](./tls-client.md). Optional, useful when many call sites share the same engine and you want to avoid passing it around.

## Methods

### init()

Initializes the singleton with a default `TlsClient(NativeTlsEngine())`. Idempotent and thread-safe.

```kotlin
import dev.kotlintls.client.Client

Client.init()
```

### getInstance(): TlsClient

Returns the initialized client. Throws `IllegalStateException` if `init()` hasn't been called.

```kotlin
val client = Client.getInstance()
```

### destroy()

Destroys all sessions and clears the singleton. Call this on shutdown.

```kotlin
Client.destroy()
```

### isReady(): Boolean

Returns `true` after `init()` and before `destroy()`.

```kotlin
if (!Client.isReady()) Client.init()
```

## Example

```kotlin
import dev.kotlintls.client.Client
import dev.kotlintls.models.SessionOptions
import dev.kotlintls.session.Session

fun main() {
    Client.init()

    val s1 = Session(Client.getInstance(), SessionOptions(sessionId = "user-a"))
    val s2 = Session(Client.getInstance(), SessionOptions(sessionId = "user-b"))

    s1.get("https://httpbin.org/get")
    s2.get("https://httpbin.org/get")

    s1.close()
    s2.close()
    Client.destroy()
}
```

## See also

- [TlsClient](./tls-client.md)
- [fetch](./fetch.md): auto-uses the singleton when initialized
