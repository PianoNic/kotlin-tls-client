# Client

Singleton that holds a shared `TlsClient`. Matches Node `initTLS()` / `destroyTLS()` / `getTLSClient()`.

## Methods

### `Client.init()`

Creates the shared `TlsClient`. Call once at startup.

```kotlin
Client.init()
```

Safe to call multiple times (no-op if already initialized).

### `Client.destroy()`

Destroys all sessions and clears the shared client.

```kotlin
Client.destroy()
```

### `Client.getInstance(): TlsClient`

Returns the shared client. Throws `IllegalStateException` if `init()` was not called.

```kotlin
val tlsClient = Client.getInstance()
```

### `Client.isReady(): Boolean`

Returns `true` if `init()` was called and the client is available.

```kotlin
if (Client.isReady()) {
    val client = Client.getInstance()
}
```

## Lifecycle

```kotlin
// At app startup
Client.init()

// Use the shared client via Session or directly
val session = Session(Client.getInstance(), SessionOptions(...))

// At app shutdown
Client.destroy()
```

## Notes

- `fetch()` automatically uses `Client.getInstance()` if the client is ready; otherwise it creates a temporary `TlsClient`.
- You don't need to use `Client` at all if you manage `TlsClient` instances yourself.

## See also

- [TlsClient](./tls-client.md)
- [Session](./session.md)
- [fetch](./fetch.md)
