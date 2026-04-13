# Building the Native Libraries

The native libraries bundled in the JAR are the Go shared libraries (`libtls_client_go.*`) built from the fork at [`PianoNic/tls-client`](https://github.com/PianoNic/tls-client), which auto-syncs with upstream [bogdanfinn/tls-client](https://github.com/bogdanfinn/tls-client).

Since the library uses JNA (not JNI), there is no C bridge to compile — only the Go shared libraries are needed.

The current bundled version is recorded in [`natives-version.txt`](../natives-version.txt).

---

## Automatic updates (CI)

The repo includes a daily GitHub Actions pipeline (`.github/workflows/update-natives.yml`) that:

1. Checks if the fork `PianoNic/tls-client` has published a new release
2. If yes — downloads the new prebuilt Go libs for all platforms
3. Commits the updated files, bumps the minor version (`v1.0.x` → `v1.1.0`), and pushes the tag
4. The normal CI pipeline then builds and publishes a new GitHub Release automatically

You don't need to do anything — updates happen on their own.

To trigger it manually: **GitHub → Actions → Update Native Libraries → Run workflow**

---

## Supported platforms

| Directory | Platform | Go lib source |
|---|---|---|
| `arm64-v8a` | Android ARM64 | Built by fork CI |
| `armeabi-v7a` | Android ARM32 | Built by fork CI |
| `linux-x86_64` | Linux x86_64 | Built by fork CI |
| `linux-aarch64` | Linux ARM64 | Built by fork CI |
| `macos-arm64` | macOS Apple Silicon | Built by fork CI |
| `macos-x86_64` | macOS Intel | Built by fork CI |
| `windows-x86_64` | Windows x64 | Built by fork CI |

---

## After updating natives

Rebuild the JAR to include the updated libraries:

```bash
./gradlew jar
```

Or push a new tag to have CI build and release it automatically:

```bash
git tag v1.x.x && git push origin v1.x.x
```
