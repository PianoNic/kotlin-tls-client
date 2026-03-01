# Getting Started

## Prerequisites

- **Kotlin 1.9+** / **JVM 21+**
- No other setup needed — native libraries are bundled in the JAR

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
    implementation("com.github.PianoNic:kotlin-tls-client:v1.0.1")
}
```

## Basic usage

### Simplest: one-shot fetch

```kotlin
import dev.kotlintls.*

fun main() {
    val resp = fetch("https://httpbin.org/get")
    println(resp.status)   // 200
    println(resp.body)
}
```

### Session: reuse cookies across requests

```kotlin
import dev.kotlintls.*

fun main() {
    Client.init()
    val session = Session(Client.getInstance(), SessionOptions(
        clientIdentifier = ClientIdentifier.CHROME_133,
        followRedirects = true
    ))

    val resp = session.get("https://httpbin.org/get")
    println(resp.status)

    session.close()
    Client.destroy()
}
```

### POST with body

```kotlin
val resp = session.post("https://httpbin.org/post", RequestOptions(
    headers = mapOf("Content-Type" to "application/json"),
    body = """{"key":"value"}"""
))
println(resp.status)
```

### Real browser fingerprint (NativeTlsEngine)

The native libraries are already bundled in the JAR — no manual setup needed.

```kotlin
val client = TlsClient(NativeTlsEngine())
val session = Session(client, SessionOptions(
    clientIdentifier = ClientIdentifier.CHROME_133
))
val resp = session.get("https://example.com")
```

## Next steps

- [Session API](./api/session.md) – All session methods and options
- [Types](./api/types.md) – Full list of request/response types
- [TLS Fingerprinting](./TLS_FINGERPRINTING.md) – How browser fingerprinting works

## Troubleshooting

**`UnsatisfiedLinkError: tls_client_jni`**
Your platform may not have a bundled native library yet. Check the [supported platforms](./api/native-engine.md#supported-platforms) list.

**`IllegalStateException: Client not initialized`**
You called `Client.getInstance()` without calling `Client.init()` first.

**Connection timeout**
Increase `timeout` in `SessionOptions` (value is in milliseconds: `timeout = 60_000`).
