# Getting Started

Install kotlin-tls-client, make your first request, and pick a browser TLS profile.

## Requirements

- Kotlin 1.9 or newer
- JDK 21 or newer
- Gradle 8.x

## Install

### Gradle (Kotlin DSL)

Add JitPack to your repositories and the dependency to your build:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// build.gradle.kts
dependencies {
    implementation("com.github.PianoNic:kotlin-tls-client:v1.0.0")
}
```

### Build from source

```bash
git clone https://github.com/PianoNic/kotlin-tls-client.git
cd kotlin-tls-client
./gradlew build
./gradlew publishToMavenLocal
```

The `downloadNatives` Gradle task fetches the Go native libraries on first build and caches them under `build/natives/`.

## Make your first request

The simplest way is `fetch`. It creates a temporary session, performs one request, and closes the session.

```kotlin
import dev.kotlintls.client.fetch
import dev.kotlintls.models.RequestMethod

fun main() {
    val response = fetch("https://httpbin.org/get", RequestMethod.GET)
    println("status: ${response.status}")
    println("body:   ${response.body.take(200)}")
}
```

`fetch` lazily creates a `TlsClient` for you. If you call `Client.init()` first, it will reuse the shared client instead.

## Pick a TLS profile

The client identifier controls the TLS handshake fingerprint (JA3) and the order of headers your client sends. The default is Chrome 133.

```kotlin
import dev.kotlintls.client.Client
import dev.kotlintls.models.ClientIdentifier
import dev.kotlintls.models.SessionOptions
import dev.kotlintls.session.Session

fun main() {
    Client.init()

    val session = Session(Client.getInstance(), SessionOptions(
        clientIdentifier = ClientIdentifier.FIREFOX_135,
        followRedirects = true,
        timeout = 30_000
    ))

    val resp = session.get("https://tls.peet.ws/api/all")
    println(resp.status)
    println(resp.body)

    session.close()
    Client.destroy()
}
```

Common profiles: `CHROME_133`, `CHROME_146`, `FIREFOX_135`, `FIREFOX_147`, `SAFARI_IOS_18_0`, `OPERA_91`. See [`ClientIdentifier`](../src/main/kotlin/dev/kotlintls/models/ClientIdentifier.kt) for the full list (~70 profiles).

If you want to understand *why* this matters and what JA3 actually is, read [TLS Fingerprinting](./tls-fingerprinting.md).

## Use a custom JA3

Pass a JA3 string instead of a preset:

```kotlin
val session = Session(Client.getInstance(), SessionOptions(
    ja3String = "771,4865-4866-4867,0-23-65281-10-11-35-16-5-13,29-23-24,0"
))
```

## Send a POST with a body and headers

```kotlin
import dev.kotlintls.client.Client
import dev.kotlintls.models.RequestOptions
import dev.kotlintls.session.Session

Client.init()
val session = Session(Client.getInstance())

val resp = session.post("https://httpbin.org/post", RequestOptions(
    body = """{"hello":"world"}""",
    headers = mapOf("Content-Type" to "application/json")
))

println(resp.status)
println(session.cookies("https://httpbin.org"))

session.close()
Client.destroy()
```

## Use a proxy

```kotlin
val session = Session(Client.getInstance(), SessionOptions(
    proxy = "http://user:pass@proxy.example.com:8080"
))
```

## Next steps

- [TLS Fingerprinting](./tls-fingerprinting.md) — Why this library exists; what JA3 is
- [Session API](./api/session.md) — All session methods and options
- [TlsClient API](./api/tls-client.md) — Low-level request payloads
- [Architecture](./architecture.md) — How the library is organized
