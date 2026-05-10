# kotlin-tls-client

[![](https://jitpack.io/v/PianoNic/kotlin-tls-client.svg)](https://jitpack.io/#PianoNic/kotlin-tls-client)

A Kotlin HTTP client with browser TLS fingerprint impersonation. Wraps the Go [tls-client](https://github.com/bogdanfinn/tls-client) library via JNA.

## Features

- Browser TLS fingerprints (Chrome, Firefox, Safari, Opera, mobile profiles)
- Session-based requests with cookie jar
- One-shot `fetch(url)` helper
- HTTP/HTTPS proxy, header order, timeouts, redirects
- Custom JA3 string support
- Slim JAR (~95 KB). Native binary published separately and dropped in by you for your target platform
- Works on JVM (Linux, macOS, Windows, FreeBSD) and Android

## Installation

The library JAR is ~95 KB of Kotlin code and does **not** bundle the native TLS engine. Two steps:

1. Add the Gradle dep:

```kotlin
dependencies {
    implementation("com.github.PianoNic:kotlin-tls-client:v2.0.0")
}
```

(plus JitPack in your repositories — see [Getting Started](./docs/getting-started.md#install)).

2. Download the native binary for your target platform from the [latest natives release](https://github.com/PianoNic/kotlin-tls-client-natives/releases/latest) and put it where your JVM can find it.

The full per-platform table (file name, where to drop it for Linux / macOS / Windows / FreeBSD / Android), Gradle wiring snippets, and a build-from-source guide all live in [Getting Started](./docs/getting-started.md).

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

`downloadNatives` pulls every platform's binary into `build/natives/` so the test suite can load the matching one. The natives are not bundled into the published JAR.

## Documentation

See the [docs/](docs/) folder for installation, the architecture overview, and the API reference.

## Credits

Wraps [bogdanfinn/tls-client](https://github.com/bogdanfinn/tls-client) (Go) via JNA.

## License

[MIT](LICENSE)
