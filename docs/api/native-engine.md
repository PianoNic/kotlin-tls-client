# NativeTlsEngine

Makes requests look like a real browser (Chrome, Firefox, etc.) by using the Go tls-client under the hood via JNI. Gives you the same TLS fingerprint (JA3) as the Go and Node tls-clients.

## Setup

None. The native libraries are bundled inside the JAR and extracted automatically at runtime.

## Usage

```kotlin
val client = TlsClient(NativeTlsEngine())
```

Everything else is identical to the default engine:

```kotlin
// Via Session
val session = Session(client, SessionOptions(
    clientIdentifier = ClientIdentifier.CHROME_133
))
val resp = session.get("https://example.com")

// Low-level
val data = client.request(RequestPayload(
    requestUrl = "https://example.com",
    tlsClientIdentifier = ClientIdentifier.CHROME_133.value
))
```

## Supported platforms

| Platform | Architecture |
|---|---|
| Android | arm64-v8a, armeabi-v7a |
| Linux | x86_64, aarch64 |
| macOS | arm64 (Apple Silicon), x86_64 (Intel) |
| Windows | x64 |

On unsupported platforms, `NativeTlsEngine` falls back to `System.loadLibrary()` — useful if you provide your own native libraries.

## How it works

```
TlsClient(NativeTlsEngine())
  → NativeLibLoader.ensureLoaded()
      → detects platform/ABI
      → extracts bundled .so/.dylib/.dll from JAR to temp dir
      → System.load(goLib), System.load(jniLib)
  → request(payload)
      → payload.toRequestJson()
      → NativeTlsEngine.nativeRequest(json)
        → JNI (tls_client_jni.c)
          → dlsym("request") in libtls_client_go
          → Go tls-client performs request with uTLS
          → returns JSON response
      → json.parseResponseJson()
```

## Native library versions

The bundled Go libraries come from [bogdanfinn/tls-client](https://github.com/bogdanfinn/tls-client). The current bundled version is tracked in [`natives-version.txt`](../../natives-version.txt) and updated automatically via the daily CI pipeline.

## Errors

**`UnsatisfiedLinkError`**
Your platform is not in the supported list above. Use `TlsClient()` (OkHttp) instead, or provide your own native libraries and call `System.loadLibrary()` before constructing `NativeTlsEngine()`.

## See also

- [TLS Fingerprinting](../TLS_FINGERPRINTING.md) – Full explanation
- [Building Natives](../building-natives.md) – How the native libraries are built and updated
