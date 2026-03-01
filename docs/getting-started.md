# Getting Started

## Prerequisites

- **Kotlin 1.9+** / **JVM 21+**
- No other setup needed for basic use (OkHttp engine)
- For real browser fingerprint: Android NDK + Go (see [TLS Fingerprinting](./TLS_FINGERPRINTING.md))

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("dev.kotlin-tls:kotlin-tls-client:1.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'dev.kotlin-tls:kotlin-tls-client:1.0.0'
}
```

### Build from source

```bash
git clone <repo>
cd kotlin-tls-client
./gradlew build
./gradlew publishToMavenLocal   # then use implementation("dev.kotlin-tls:kotlin-tls-client:1.0.0")
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

## Project structure

```
your-project/
├── build.gradle.kts
└── src/main/kotlin/
    └── Main.kt
```

For Android with native engine (real browser fingerprint):
```
your-app/
├── src/main/
│   ├── kotlin/...
│   └── jniLibs/
│       ├── arm64-v8a/
│       │   ├── libtls_client_go.so
│       │   └── libtls_client_jni.so
│       └── armeabi-v7a/
│           ├── libtls_client_go.so
│           └── libtls_client_jni.so
```

## Next steps

- [Session API](./api/session.md) – All session methods and options
- [Types](./api/types.md) – Full list of request/response types
- [TLS Fingerprinting](./TLS_FINGERPRINTING.md) – Making requests look like a real browser

## Troubleshooting

**`UnsatisfiedLinkError: tls_client_jni`**
You called `NativeTlsEngine()` but the native `.so` files aren't loaded. Either load them manually (`System.loadLibrary(...)`) or use `TlsClient()` without an engine if you don't need browser fingerprinting.

**`IllegalStateException: Client not initialized`**
You called `Client.getInstance()` without calling `Client.init()` first.

**Connection timeout**
Increase `timeout` in `SessionOptions` (value is in milliseconds: `timeout = 60_000`).
