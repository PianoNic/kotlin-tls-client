# What you need to do

## For basic HTTP requests

Nothing. Add the dependency and use `TlsClient()` or `Session`:

```kotlin
val resp = fetch("https://example.com")
```

## For real browser fingerprint (JA3)

Nothing. The native libraries for all supported platforms are already bundled inside the JAR.

Just use `NativeTlsEngine()`:

```kotlin
val client = TlsClient(NativeTlsEngine())
val session = Session(client, SessionOptions(
    clientIdentifier = ClientIdentifier.CHROME_133
))
val resp = session.get("https://example.com")
```

The library automatically detects your platform, extracts the right native library from the JAR, and loads it. No `System.loadLibrary()` calls, no `.so` files to manage.

**Supported platforms:** Android (arm64, arm32), Linux (x86_64, aarch64), macOS (arm64, x86_64), Windows (x64).

## Summary

| Goal | What to do |
|---|---|
| Standard HTTP | `TlsClient()` — zero setup |
| Real browser fingerprint | `TlsClient(NativeTlsEngine())` — zero setup |
| Unsupported platform | Use `TlsClient()` or open an issue |
