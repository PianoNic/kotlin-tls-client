# NativeTlsEngine

Makes requests look like a real browser (Chrome, Firefox, etc.) by using the Go tls-client under the hood via JNI. This gives you the same TLS fingerprint (JA3) as the Go and Node tls-clients.

## When you need this

By default `TlsClient()` uses OkHttp — the TLS handshake looks like a standard Java/Android app. Some sites detect this and block it.

With `NativeTlsEngine`, the Go tls-client handles the connection and sends a realistic browser-like TLS handshake.

## Setup

You need two `.so` files on the device:

| File | What it is |
|------|-----------|
| `libtls_client_go.so` | Go tls-client (the actual browser-mimicking library) |
| `libtls_client_jni.so` | C bridge (built from `jni/tls_client_jni.c`) |

### Android (pre-built)

The `.so` files for `arm64-v8a` and `armeabi-v7a` are already built and placed in `src/main/jniLibs/` by the `build_android.sh` script. Just include the module and load them at startup.

### Build yourself

See [What You Need To Do](../WHAT_YOU_NEED_TO_DO.md) for step-by-step build instructions.

## Usage

### 1. Load the libraries

In `Application.onCreate()` (Android) or at the start of your app:

```kotlin
System.loadLibrary("tls_client_go")
System.loadLibrary("tls_client_jni")
```

### 2. Use the native engine

```kotlin
val client = TlsClient(NativeTlsEngine())
```

Everything else is identical to the default engine:

```kotlin
// Low-level
val data = client.request(RequestPayload(
    requestUrl = "https://example.com",
    tlsClientIdentifier = ClientIdentifier.CHROME_133.value
))

// Via Session
val session = Session(client, SessionOptions(
    clientIdentifier = ClientIdentifier.CHROME_133
))
val resp = session.get("https://example.com")

// Via fetch (after Client.init with native engine)
```

## How it works

```
TlsClient(NativeTlsEngine())
  → request(payload)
    → payload.toRequestJson()    // serialize to JSON
    → NativeTlsEngine.nativeRequest(json)
      → JNI (tls_client_jni.c)
        → dlsym("request") in libtls_client_go.so
        → Go tls-client performs the request with uTLS
        → returns JSON response string
    → json.parseResponseJson()
```

## Errors

**`UnsatisfiedLinkError: Failed to load tls_client_jni`**
The `.so` files are missing or not loaded. Make sure:
1. The files are in `jniLibs/arm64-v8a/` (and other ABIs).
2. You called `System.loadLibrary("tls_client_go")` before `System.loadLibrary("tls_client_jni")`.

## See also

- [TLS Fingerprinting](../TLS_FINGERPRINTING.md) – Full explanation
- [What You Need To Do](../WHAT_YOU_NEED_TO_DO.md) – Build steps
- [jni/README.md](../../jni/README.md) – JNI bridge details
