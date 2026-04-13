# kotlin-tls-client

[![](https://jitpack.io/v/PianoNic/kotlin-tls-client.svg)](https://jitpack.io/#PianoNic/kotlin-tls-client)

A Kotlin HTTP client library with the **same API as the Go [tls-client](https://github.com/bogdanfinn/tls-client) and Node [node-tls-client](https://github.com/bogdanfinn/node-tls-client)**. Drop-in replacement for projects that use those libraries.

> 📚 [Documentation](./docs/) • [API Reference](./docs/README.md#api-reference)

## Features

Session-based requests • One-shot fetch • TLS profile identifiers (Chrome, Firefox, Safari, …)
Cookie jar • Proxy • Header order • Custom TLS / JA3 API • Timeout • Redirects
Real browser fingerprint via native engine (Go tls-client + JNA)

## Installation

**Step 1** — Add JitPack to your repositories:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**Step 2** — Add the dependency:

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.PianoNic:kotlin-tls-client:v1.0.0")
}
```

<details>
<summary>Gradle (Groovy)</summary>

```groovy
// settings.gradle
maven { url 'https://jitpack.io' }

// build.gradle
implementation 'com.github.PianoNic:kotlin-tls-client:v1.0.0'
```
</details>

## Quick Start

### One-shot fetch

```kotlin
import dev.kotlintls.*

val response = fetch("https://httpbin.org/get")
println(response.status)  // 200
println(response.body)
```

### Session (Node-style)

```kotlin
import dev.kotlintls.*

Client.init()
val session = Session(Client.getInstance(), SessionOptions(
    clientIdentifier = ClientIdentifier.CHROME_133,
    followRedirects = true,
    timeout = 30_000
))

val resp = session.get("https://httpbin.org/get", RequestOptions(
    headers = mapOf("Accept" to "application/json")
))
println(resp.status)
println(session.cookies("https://httpbin.org"))
session.close()
Client.destroy()
```

### Low-level request (Go/FFI-style)

```kotlin
import dev.kotlintls.*

val client = TlsClient()
val data = client.request(RequestPayload(
    requestUrl = "https://httpbin.org/post",
    requestMethod = RequestMethod.POST,
    requestBody = """{"key":"value"}""",
    tlsClientIdentifier = ClientIdentifier.CHROME_133.value,
    sessionId = "my-session",
    headers = mapOf("Content-Type" to "application/json")
))
println(data.status)
client.destroySession(DestroySessionPayload("my-session"))
```

## Documentation

- [Getting Started](./docs/getting-started.md)
- [Architecture](./docs/architecture.md)
- [TLS Fingerprinting](./docs/TLS_FINGERPRINTING.md)

**API:** [TlsClient](./docs/api/tls-client.md) • [Session](./docs/api/session.md) • [fetch](./docs/api/fetch.md) • [Client](./docs/api/client.md) • [Types](./docs/api/types.md) • [NativeTlsEngine](./docs/api/native-engine.md)

## TLS fingerprinting

By default the library uses OkHttp — requests don't mimic a specific browser. To get a **real browser fingerprint** (Chrome, Firefox, etc.), use the native engine:

```kotlin
val client = TlsClient(NativeTlsEngine())
```

The native Go libraries are bundled in the JAR for all supported platforms — no manual setup needed. JNA handles loading them automatically. See [TLS Fingerprinting](./docs/TLS_FINGERPRINTING.md) for the full explanation.

## Attribution

Based on:
- **[tls-client](https://github.com/bogdanfinn/tls-client)** by [bogdanfinn](https://github.com/bogdanfinn)
- **[node-tls-client](https://github.com/bogdanfinn/node-tls-client)** by [bogdanfinn](https://github.com/bogdanfinn)

## License

MIT
