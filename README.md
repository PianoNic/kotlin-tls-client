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

The library JAR is code only (~100 KB) and does not bundle the native TLS engine. You add the Kotlin dep, then drop the matching native binary onto your runtime path. See [Getting Started](./docs/getting-started.md) for the step-by-step.

### 1. Add the Kotlin dependency

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
    implementation("com.github.PianoNic:kotlin-tls-client:v2.0.0")
}
```

### 2. Drop in the native binary for your platform

Download the zip for your target from the [latest natives release](https://github.com/PianoNic/kotlin-tls-client-natives/releases/latest) and unpack it.

| Target | Zip | File inside | Where to put it |
|---|---|---|---|
| Linux x86_64 | `linux-x86_64.zip` | `libtls_client_go.so` | a directory on `java.library.path` (or `LD_LIBRARY_PATH`) |
| Linux ARM64 | `linux-aarch64.zip` | `libtls_client_go.so` | (same) |
| Linux ARMv7 | `linux-arm.zip` | `libtls_client_go.so` | (same) |
| macOS arm64 | `macos-arm64.zip` | `libtls_client_go.dylib` | a directory on `java.library.path` |
| macOS Intel | `macos-x86_64.zip` | `libtls_client_go.dylib` | (same) |
| Windows x86_64 | `windows-x86_64.zip` | `tls_client_go.dll` | a directory on `PATH` or `java.library.path` |
| Windows ARM64 | `windows-arm64.zip` | `tls_client_go.dll` | (same) |
| FreeBSD x86_64 | `freebsd-x86_64.zip` | `libtls_client_go.so` | a directory on `java.library.path` |
| Android arm64 | `android-arm64-v8a.zip` | `libtls_client_go.so` | `app/src/main/jniLibs/arm64-v8a/` |
| Android armv7 | `android-armeabi-v7a.zip` | `libtls_client_go.so` | `app/src/main/jniLibs/armeabi-v7a/` |
| Android x86_64 | `android-x86_64.zip` | `libtls_client_go.so` | `app/src/main/jniLibs/x86_64/` |
| Android x86 | `android-x86.zip` | `libtls_client_go.so` | `app/src/main/jniLibs/x86/` |

Run with the path set, for example:

```bash
java -Djava.library.path=/path/to/natives -jar your-app.jar
```

For an Android module, the `.so` only needs to sit under `jniLibs/<abi>/` before the APK is built; the Android Gradle Plugin handles the rest.

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
