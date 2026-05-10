# NativeTlsEngine

The default (and only shipped) implementation of [`TlsClientEngine`](../architecture.md#port--tlsclientengine). Loads the Go [tls-client](https://github.com/bogdanfinn/tls-client) shared library via JNA and forwards each call as a JSON string. This is what gives you a real browser TLS fingerprint.

For background on *why* this matters, see [TLS Fingerprinting](../tls-fingerprinting.md).

## Usage

`TlsClient()` already uses it, you usually don't need to construct one yourself.

```kotlin
import dev.kotlintls.TlsClient
import dev.kotlintls.engine.NativeTlsEngine

val explicit = TlsClient(NativeTlsEngine())  // same as TlsClient()
```

## How the native lib is loaded

1. On first use, `NativeLibLoader` detects your OS + architecture.
2. It calls `Native.load("tls_client_go", ...)` (JNA's by-name lookup), which searches:
   - `java.library.path`
   - the OS library path (`LD_LIBRARY_PATH` on Linux/FreeBSD, `DYLD_LIBRARY_PATH` on macOS, `PATH` on Windows)
   - on Android: the APK's `lib/<abi>/` directory (populated from `jniLibs/`)
3. The handle is cached for the rest of the JVM lifetime.

The JAR does **not** carry the native binary. You download the matching zip from [kotlin-tls-client-natives releases](https://github.com/PianoNic/kotlin-tls-client-natives/releases/latest) and put the file where the JVM can find it. See the [getting-started guide](../getting-started.md#install) for the per-platform table.

## Supported platforms

| OS | Architectures |
|---|---|
| Android | `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` |
| Windows | `x86_64`, `arm64` |
| Linux | `x86_64`, `aarch64`, `arm` (32-bit) |
| macOS | `arm64`, `x86_64` |
| FreeBSD | `x86_64` |

## Build and versioning

The native libraries aren't stored in git. The Gradle `downloadNatives` task fetches a versioned bundle on first build. The version is pinned in `natives-version.txt` and bumped automatically when the [`PianoNic/kotlin-tls-client-natives`](https://github.com/PianoNic/kotlin-tls-client-natives) fork releases (the fork auto-syncs with upstream `bogdanfinn/tls-client`).

## Implementing a different engine

Anything implementing [`TlsClientEngine`](../architecture.md#port--tlsclientengine) works as a drop-in replacement, useful for unit tests:

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

- [Architecture](../architecture.md): where this fits
- [TLS Fingerprinting](../tls-fingerprinting.md): why a native engine is needed
