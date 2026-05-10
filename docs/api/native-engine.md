# NativeTlsEngine

The default (and only shipped) implementation of [`TlsClientEngine`](../architecture.md#port--tlsclientengine). Loads the Go [tls-client](https://github.com/bogdanfinn/tls-client) shared library via JNA and forwards each call as a JSON string. This is what gives you a real browser TLS fingerprint.

For background on *why* this matters, see [TLS Fingerprinting](../tls-fingerprinting.md).

## Usage

`TlsClient()` already uses it — you usually don't need to construct one yourself.

```kotlin
import dev.kotlintls.TlsClient
import dev.kotlintls.engine.NativeTlsEngine

val explicit = TlsClient(NativeTlsEngine())  // same as TlsClient()
```

## How the native lib is loaded

1. On first use, `NativeLibLoader` detects your OS + architecture.
2. It extracts the matching shared library from `dev/kotlintls/natives/<platform>/` inside the JAR to a temp directory.
3. JNA loads it.
4. The handle is cached for the rest of the JVM lifetime.

On Android, `System.loadLibrary("tls_client_go")` is used instead, because Android's W^X policy blocks executing files extracted to the temp dir.

## Supported platforms

| OS | Architectures |
|---|---|
| Linux | `x86_64`, `aarch64` |
| macOS | `x86_64`, `arm64` |
| Windows | `x86_64` |
| Android | `arm64-v8a`, `armeabi-v7a` |

## Build and versioning

The native libraries aren't stored in git. The Gradle `downloadNatives` task fetches a versioned bundle on first build. The version is pinned in `natives-version.txt` and bumped automatically when the [`PianoNic/tls-client`](https://github.com/PianoNic/tls-client) fork releases (the fork auto-syncs with upstream `bogdanfinn/tls-client`).

## Implementing a different engine

Anything implementing [`TlsClientEngine`](../architecture.md#port--tlsclientengine) works as a drop-in replacement — useful for unit tests:

```kotlin
import dev.kotlintls.TlsClient
import dev.kotlintls.engine.TlsClientEngine

class FakeEngine : TlsClientEngine {
    override fun request(requestJson: String) = """{"status":200,"body":"ok"}"""
    override fun destroySession(payloadJson: String) = """{"id":"x","success":true}"""
    override fun getCookiesFromSession(payloadJson: String) = """{"id":"x","cookies":[]}"""
    override fun destroyAll() = """{"id":"x","success":true}"""
}

val client = TlsClient(FakeEngine())
```

## See also

- [Architecture](../architecture.md) — Where this fits
- [TLS Fingerprinting](../tls-fingerprinting.md) — Why a native engine is needed
