# TLS Fingerprinting, A Short Lesson

This page explains what TLS fingerprinting actually is, why ordinary HTTP clients get blocked, and what `kotlin-tls-client` does about it. No prior cryptography knowledge needed, just the basics of how HTTPS works.

## The 30-second version

When you open `https://example.com`, your browser performs a **TLS handshake** with the server before any HTTP traffic moves. The very first message your client sends, the **ClientHello**, is essentially a long list of "here's what I support: these cipher suites, these extensions, in this order, with these parameters."

Different clients (Chrome, Firefox, Safari, OkHttp, curl, Python's `requests`, Java's `HttpClient`) build that list **differently**. The differences are stable, deterministic, and visible to the server *before any HTTP header is even sent*.

That stable shape is called a **TLS fingerprint**. The server can read it, hash it, compare it to a known list, and decide:

> "This claims to be Chrome 133 in the User-Agent, but the TLS fingerprint matches OkHttp. Block."

## How servers detect a non-browser

A typical bot-protection service (Cloudflare, Akamai, PerimeterX, DataDome) checks at least three layers:

| Layer | What it sees | What gives you away |
|---|---|---|
| **TLS** | ClientHello bytes | Cipher list, extensions, curves, versions, GREASE values, extension order |
| **HTTP/2** | SETTINGS, WINDOW_UPDATE, header frames | Stream priorities, pseudo-header order, HPACK table size |
| **HTTP** | Request line + headers | Header order, casing, exact `User-Agent`, presence of `sec-ch-ua-*` |

Even if you set `User-Agent: Mozilla/5.0 ... Chrome/133...`, a real Chrome's TLS handshake looks completely different from your Java/Kotlin HTTP client's. Servers don't trust the User-Agent, they trust what they can measure themselves.

## JA3, the most common TLS fingerprint format

**JA3** is a way of summarizing the ClientHello as a short string. Five fields, comma-separated:

```
TLSVersion,Ciphers,Extensions,EllipticCurves,EllipticCurvePointFormats
```

Example for Chrome 133:

```
771,4865-4866-4867-49195-49199-49196-49200-52393-52392-49171-49172-156-157-47-53,0-23-65281-10-11-35-16-5-13-18-51-45-43-27-17513,29-23-24,0
```

That string gets hashed (MD5) and used as a single ID. Cloudflare and friends keep curated lists of "these JA3 hashes are real Chrome on Windows", "these are headless Chrome", "these are Go's `net/http`", and route accordingly.

JA4 is a newer, more granular variant, same idea, more fields. This library covers both via the underlying Go `tls-client`.

## Why a plain Kotlin/Java client can't fake it

Java's `javax.net.ssl` and Kotlin clients built on it (OkHttp, Ktor's Java engine, Apache HttpClient) all delegate the handshake to the JDK's TLS stack. You can configure cipher suites a little, but you **cannot** reorder extensions, drop GREASE, change the key share format, or match the exact byte sequence Chrome produces. The JDK gives you a JDK fingerprint, not a Chrome one.

Some libraries (BoringSSL bindings, Conscrypt) get closer, but matching a *current* browser's handshake exactly, including quirks that change every few releases, is a moving target. That's why most TLS-impersonation libraries don't reimplement TLS in Kotlin/Java; they wrap a battle-tested fork of Go's `crypto/tls` instead.

## How `kotlin-tls-client` handles it

This library wraps **[bogdanfinn/tls-client](https://github.com/bogdanfinn/tls-client)**, a fork of Go's `crypto/tls` with explicit support for emitting ClientHello bytes that match real browser builds. It's the same engine many Python TLS-impersonation tools use.

```
your code
   │
   ▼  (Kotlin types)
TlsClient → NativeTlsEngine → JNA → tls_client_go.{so,dll,dylib}
                                          │
                                          ▼  (real Chrome 133 ClientHello bytes)
                                       server
```

The native library is published separately from the Kotlin JAR. You download the matching `.so`/`.dll`/`.dylib` from the [natives release page](https://github.com/PianoNic/kotlin-tls-client-natives/releases/latest), drop it where the JVM can find it (`java.library.path`, or `jniLibs/<abi>/` on Android), and JNA loads it on first use. See the [install guide](./getting-started.md#install) for the per-platform table.

You select a fingerprint by name:

```kotlin
SessionOptions(clientIdentifier = ClientIdentifier.CHROME_133)
```

Or pass a JA3 string directly (and configure HTTP/2 settings, header priorities, etc.):

```kotlin
SessionOptions(
    ja3String = "771,4865-4866-4867,0-23-65281-10-11-35-16-5-13,29-23-24,0",
    pseudoHeaderOrder = listOf(":method", ":authority", ":scheme", ":path"),
    h2Settings = mapOf("HEADER_TABLE_SIZE" to 65536u, "INITIAL_WINDOW_SIZE" to 6291456u)
)
```

## What this library does *not* do

A correct TLS fingerprint is **necessary but not sufficient** to look like a real browser. Detection systems also look at:

- **JavaScript challenges**, `_cf_chl_opt`, hCaptcha, Turnstile. You'd need a real browser (Playwright, Puppeteer) for these.
- **Behavioral signals**, mouse movement, request timing, navigation patterns.
- **IP reputation**, datacenter IPs are flagged regardless of fingerprint.
- **Cookies and tokens**, `__cf_bm`, `cf_clearance`, etc. accumulate over a real session.

This library handles the **transport layer**, TLS, HTTP/2, HTTP/1.1 framing and headers. Above that, you're on your own.

## Verifying it works

Hit a fingerprinting echo service and inspect what the server saw:

```kotlin
val resp = fetch("https://tls.peet.ws/api/all")
println(resp.body)
```

Look for `tls.ja3_hash`, `tls.peetprint`, and `http2.akamai_fingerprint` in the response. They should match Chrome's known values, not OkHttp's or Java's.

## Further reading

- **JA3**, original blog post: <https://engineering.salesforce.com/tls-fingerprinting-with-ja3-and-ja3s-247362855967/>
- **JA4+**, newer family of fingerprints: <https://github.com/FoxIO-LLC/ja4>
- **`bogdanfinn/tls-client`**, the engine this library wraps: <https://github.com/bogdanfinn/tls-client>
- **Cloudflare's perspective**, "Browser fingerprinting": <https://developers.cloudflare.com/bots/concepts/bot/>
