# TLS Fingerprinting (JA3) — simple guide

## What's going on?

Some websites check **how** your app connects (not just the URL or headers). That check is called **TLS fingerprinting** (or JA3).

- The **Go and Node** tls-clients can **pretend to be a real browser** (Chrome, Firefox, etc.) so the site sees a normal browser fingerprint.
- This Kotlin library uses the same Go tls-client under the hood via JNI, so you get the **same fingerprint** out of the box.

---

## Option 1: Real browser fingerprint — built in (recommended)

The native libraries are **already bundled in the JAR**. No manual setup, no extra files.

Just use `NativeTlsEngine()`:

```kotlin
val client = TlsClient(NativeTlsEngine())
val session = Session(client, SessionOptions(
    clientIdentifier = ClientIdentifier.CHROME_133
))
val resp = session.get("https://example.com")
```

Supported platforms: Android (arm64, arm32), Linux (x86_64, aarch64), macOS (arm64, x86_64), Windows (x64).

---

## Option 2: Without native engine (OkHttp only)

Use `TlsClient()` with no engine. Requests use standard OkHttp — the TLS handshake will look like a Java/Android app, not a browser.

Good for: most APIs, testing, when the site doesn't check TLS fingerprints.

```kotlin
val client = TlsClient()   // uses OkHttp, no native libs
```

---

## Option 3: Server-side fingerprinting (no native on device)

If you can't or don't want native libraries on the device:

- Run the **Go or Node** tls-client on **your server**
- Your app sends the request to your server, the server makes the real browser-fingerprinted request and returns the result

The fingerprint happens on the server — no native libraries on the device.

---

## Quick summary

| What you want | What to do |
|---|---|
| Real browser fingerprint, zero setup | `TlsClient(NativeTlsEngine())` — natives are bundled |
| Standard HTTP, no native | `TlsClient()` |
| Fingerprint without native on device | Run Go/Node tls-client on your server |
