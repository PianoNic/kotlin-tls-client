# Architecture

A 30-second overview of how kotlin-tls-client is structured.

```
Your application
       |
       v
   fetch() / Session.get/post/...        ← convenience layer
       |
       v
    TlsClient.request(payload)           ← public entrypoint
       |
       v
   TlsClientEngine (interface)            ← port
       |
       v
   NativeTlsEngine                        ← adapter
       |
       v
   GoTlsClient (JNA)                      ← FFI to Go shared lib
       |
       v
   tls_client_go.{so,dll,dylib}           ← bogdanfinn/tls-client
```

## Package layout

```
dev.kotlintls/
├── TlsClient.kt              ← only entrypoint at the root
├── models/                   ← public data classes + enums
│   ├── ClientIdentifier, RequestMethod
│   ├── RequestPayload, RequestOptions
│   ├── ResponseData, Cookie
│   ├── SessionOptions, SessionPayloads
│   └── CustomTlsClient, TransportOptions, PriorityFrame
├── session/                  ← high-level session wrapper
│   ├── Session.kt
│   └── Response.kt
├── client/                   ← convenience helpers
│   ├── Client.kt             (process-wide singleton)
│   └── Fetch.kt              (one-shot fetch)
├── engine/                   ← port + native adapter
│   ├── TlsClientEngine.kt
│   └── NativeTlsEngine.kt
└── internal/                 ← impl details (also `internal` keyword)
    ├── GoTlsClient.kt        (JNA interface)
    ├── NativeLibLoader.kt    (locates and loads the native lib at runtime)
    ├── JsonDtos.kt           (wire DTOs for the Go FFI)
    └── JsonMappers.kt        (public model ↔ wire DTO + Gson)
```

## Layers

### Entrypoint, `TlsClient`

A thin façade that takes a `RequestPayload`, serializes it to JSON, hands it to a `TlsClientEngine`, and parses the response. It owns no transport or fingerprinting logic itself, it just orchestrates.

### Convenience, `fetch`, `Client`, `Session`

- `fetch(url)` creates a temporary session for one call.
- `Client` is a process-wide singleton holding a default `TlsClient`.
- `Session` keeps a `sessionId` and per-session config so the underlying Go library can persist cookies and connection state.

### Port, `TlsClientEngine`

A four-method interface (`request`, `destroySession`, `getCookiesFromSession`, `destroyAll`) defined entirely in JSON strings. This is the seam: the public API doesn't depend on JNA, Go, or any specific TLS library.

### Adapter, `NativeTlsEngine`

The only implementation that ships. It loads the Go shared library via `NativeLibLoader` and forwards each call through JNA.

### Internals, `internal/`

- `GoTlsClient`, JNA `Library` interface, four C-string functions.
- `NativeLibLoader`: detects OS+arch, then asks JNA to load the matching shared library by name. The library is **not** bundled in the JAR. Users place the `.so`/`.dll`/`.dylib` for their platform on `java.library.path` (or `jniLibs/<abi>/` on Android) and JNA's standard search resolves it.
- `JsonDtos` + `JsonMappers`, anti-corruption layer between Kotlin models and the Go FFI's JSON shape. Gson lives here and nowhere else.

## How a request flows

1. You call `session.get(url)` (or `client.request(payload)` directly).
2. `Session` builds a `RequestPayload` merging session config and per-request options.
3. `TlsClient.request` calls `payload.toRequestJson()` (in `internal`) to produce the FFI JSON string.
4. The string goes to `NativeTlsEngine.request(json)`, which forwards to `GoTlsClient.request(json)` via JNA.
5. The Go library performs the TLS handshake with the requested fingerprint, sends the request, and returns a JSON response string.
6. `String.parseResponseJson()` (in `internal`) turns it back into a `ResponseData`.
7. `Session` wraps it in a `Response` and returns.

## Native libraries

Native libs are not stored in git. The Gradle `downloadNatives` task pulls a versioned bundle from the [PianoNic/kotlin-tls-client-natives](https://github.com/PianoNic/kotlin-tls-client-natives) fork (which auto-syncs with upstream `bogdanfinn/tls-client`) and caches it under `build/natives/`. The version is pinned in `natives-version.txt` and bumped automatically by PR when the fork releases.
