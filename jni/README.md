# JNI Bridge

`tls_client_jni.c` is the C bridge between Kotlin/JVM and the Go tls-client shared library.

## How it works

```
Kotlin (NativeTlsEngine)
  → JNI call
    → tls_client_jni.c
      → dlopen / LoadLibrary (platform-specific)
        → resolves "request", "destroySession", etc. from libtls_client_go
        → calls Go function, returns JSON string
  → back to Kotlin
```

The bridge uses `dlopen`/`dlsym` on Linux/macOS and `LoadLibrary`/`GetProcAddress` on Windows, selected via `#ifdef _WIN32`.

## You don't need to build this yourself

The compiled JNI bridge for all platforms is bundled in the JAR and loaded automatically by `NativeLibLoader`. See [building-natives.md](../docs/building-natives.md) for how it gets built.

## Go library exports required

The Go tls-client must export these C symbols:

| Symbol | Signature |
|---|---|
| `request` | `char* request(const char*)` |
| `destroySession` | `char* destroySession(const char*)` |
| `getCookiesFromSession` | `char* getCookiesFromSession(const char*)` |
| `destroyAll` | `char* destroyAll(void)` |
