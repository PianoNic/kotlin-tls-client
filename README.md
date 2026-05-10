# kotlin-tls-client

[![](https://jitpack.io/v/PianoNic/kotlin-tls-client.svg)](https://jitpack.io/#PianoNic/kotlin-tls-client)

A Kotlin HTTP client with browser TLS fingerprint impersonation. Wraps the Go [tls-client](https://github.com/bogdanfinn/tls-client) library via JNA.

## Features

- Browser TLS fingerprints (Chrome, Firefox, Safari, Opera, mobile profiles)
- Session-based requests with cookie jar
- One-shot `fetch(url)` helper
- HTTP/HTTPS proxy, header order, timeouts, redirects
- Custom JA3 string support
- Native Go library bundled in the JAR — no manual setup
- Works on JVM (Linux, macOS, Windows, FreeBSD) and Android

## Installation

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

## Quick Start

```kotlin
import dev.kotlintls.client.fetch
import dev.kotlintls.models.RequestMethod

val response = fetch("https://httpbin.org/get", RequestMethod.GET)
println(response.status)
println(response.body)
```

## Building from Source

```bash
./gradlew build
```

The Go native libraries are downloaded automatically by the `downloadNatives` Gradle task.

## Documentation

See the [docs/](docs/) folder for installation, the architecture overview, and the API reference.

## Credits

Wraps [bogdanfinn/tls-client](https://github.com/bogdanfinn/tls-client) (Go) via JNA.

## License

[MIT](LICENSE)
