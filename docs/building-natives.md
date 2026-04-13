# Native Libraries

The native Go shared libraries (`libtls_client_go.*`) are **not stored in the repo**. They are downloaded automatically at build time from the fork at [`PianoNic/tls-client`](https://github.com/PianoNic/tls-client), which auto-syncs with upstream [bogdanfinn/tls-client](https://github.com/bogdanfinn/tls-client).

Since the library uses JNA (not JNI), there is no C bridge to compile — only the Go shared libraries are needed.

The pinned version is in [`natives-version.txt`](../natives-version.txt).

---

## How it works

1. `./gradlew build` triggers the `downloadNatives` task
2. It downloads platform zips from `PianoNic/tls-client` GitHub releases
3. Extracts them to `build/natives/dev/kotlintls/natives/{platform}/`
4. They get bundled into the JAR via `processResources`

Downloads are cached in `build/natives/` — they only download once until you run `./gradlew clean`.

---

## Updating to a new version

When a new version of tls-client is released:

1. The fork's CI auto-syncs and builds new binaries
2. The `update-natives.yml` workflow opens a PR bumping `natives-version.txt`
3. Merge the PR → next build downloads the new binaries

To bump manually:

```bash
echo "1.15.0" > natives-version.txt
./gradlew clean build
```

---

## Supported platforms

| Directory | Platform |
|---|---|
| `linux-x86_64` | Linux x86_64 |
| `linux-aarch64` | Linux ARM64 |
| `windows-x86_64` | Windows x64 |
| `macos-arm64` | macOS Apple Silicon |
| `macos-x86_64` | macOS Intel |
| `arm64-v8a` | Android ARM64 |
| `armeabi-v7a` | Android ARM32 |
