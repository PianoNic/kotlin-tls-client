# kotlin-tls-client

[![](https://jitpack.io/v/PianoNic/kotlin-tls-client.svg)](https://jitpack.io/#PianoNic/kotlin-tls-client)

A Kotlin HTTP client with browser TLS fingerprint impersonation. Wraps the Go [tls-client](https://github.com/bogdanfinn/tls-client) library via JNA.

## Features

- Browser TLS fingerprints (Chrome, Firefox, Safari, Opera, mobile profiles)
- Session-based requests with cookie jar
- One-shot `fetch(url)` helper
- HTTP/HTTPS proxy, header order, timeouts, redirects
- Custom JA3 string support
- Native Go library bundled in the JAR, no manual setup
- Works on JVM (Linux, macOS, Windows, FreeBSD) and Android

## Installation

Two JAR variants ship per release. Pick the one that matches your distribution model.

| Variant | Size | When to use |
|---|---|---|
| `kotlin-tls-client:v2.0.0` (default, slim) | ~95 KB | You build for one target at a time (server deploy, dev machine, single-arch container). You provide the matching native binary yourself. |
| `kotlin-tls-client:v2.0.0:all` (fat) | ~80 MB | Your app ships as one JAR that has to run on multiple host OSes (Compose Desktop, JavaFX bundle, multi-arch Docker). All 12 platform natives are bundled and the loader picks the right one at runtime. |

### Slim (default)

#### 1. Add the Kotlin dependency

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

#### 2. Drop in the native binary for your platform

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

### Fat (multi-platform)

If your app distribution has to run on multiple host OSes, use the `:all` classifier. Every native is bundled; the loader extracts the matching one at first use.

```kotlin
dependencies {
    implementation("com.github.PianoNic:kotlin-tls-client:v2.0.0:all")
}
```

No `java.library.path` setup. Just works on Linux, macOS, Windows, FreeBSD: the loader extracts the matching native to a temp directory at first call.

**Android caveat**: the Android Gradle Plugin only auto-extracts natives from JAR dependencies if they live at `jni/<abi>/lib*.so`. Our fat JAR uses `dev/kotlintls/natives/<platform>/`, so AGP won't pick them up. On Android you still need to drop the matching `.so` into `app/src/main/jniLibs/<abi>/` (slim approach), even when the rest of your build uses the fat JAR.

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
