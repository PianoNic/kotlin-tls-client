# Local Development

How to clone, build, test, and run kotlin-tls-client on your machine.

## Prerequisites

- **JDK 21+** (e.g., [Temurin](https://adoptium.net/))
- **Git**
- Internet connection (native libraries are downloaded at build time)

## Clone and build

```bash
git clone https://github.com/PianoNic/kotlin-tls-client.git
cd kotlin-tls-client
./gradlew build
```

The first build downloads ~40MB of native libraries from [PianoNic/tls-client releases](https://github.com/PianoNic/tls-client/releases). Subsequent builds reuse the cached files in `build/natives/`.

To force a fresh download:

```bash
./gradlew clean build
```

## Run tests

```bash
./gradlew test
```

Tests include both OkHttp fallback and native engine (JNA) tests. All tests hit `httpbin.org` so you need an internet connection.

## Project structure

```
kotlin-tls-client/
├── src/main/kotlin/dev/kotlintls/
│   ├── TlsClient.kt          # Public API entry point
│   ├── TlsClientEngine.kt    # Engine interface
│   ├── NativeTlsEngine.kt    # JNA-based engine (calls Go library)
│   ├── GoTlsClient.kt        # JNA interface (4 Go function bindings)
│   ├── NativeLibLoader.kt    # Extracts + loads native libs per platform
│   ├── ClientIdentifier.kt   # Browser TLS profile enum
│   └── types.kt              # Request/response data classes
├── src/test/kotlin/dev/kotlintls/
│   └── TlsClientTest.kt      # Unit + integration tests
├── build.gradle.kts           # Build config (includes downloadNatives task)
├── natives-version.txt        # Pinned tls-client version (e.g., 1.14.0)
└── docs/                      # Documentation
```

## How native libraries work

Native Go binaries are **not** stored in git. The Gradle `downloadNatives` task:

1. Reads the version from `natives-version.txt`
2. Downloads platform zips from `PianoNic/tls-client` GitHub releases
3. Extracts them to `build/natives/`
4. Bundles them into the JAR via `processResources`

See [building-natives.md](./building-natives.md) for more details.

## Common tasks

| Task | Command |
|------|---------|
| Build JAR | `./gradlew build` |
| Run tests | `./gradlew test` |
| Clean + rebuild | `./gradlew clean build` |
| Only download natives | `./gradlew downloadNatives` |
| Publish to local Maven | `./gradlew publishToMavenLocal` |

## Bumping the native library version

When a new version of tls-client is available:

```bash
echo "1.15.0" > natives-version.txt
./gradlew clean build
```

This is automated via CI — the `update-natives.yml` workflow opens a PR when a new version is detected.

## Troubleshooting

**Build fails with "Connection reset" or download errors**
GitHub rate-limits unauthenticated downloads. Wait a minute and retry, or set `GITHUB_TOKEN` in your environment.

**`UnsatisfiedLinkError` at runtime**
The native library for your platform wasn't found. Check that `build/natives/dev/kotlintls/natives/` contains your platform directory after building.

**Tests fail with timeouts**
Tests hit `httpbin.org` — check your internet connection. You can also increase timeout by modifying the test payloads.
