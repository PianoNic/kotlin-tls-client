# Building the Native Libraries

The native libraries bundled in the JAR come from two sources:

| Library | Source |
|---|---|
| `libtls_client_go.*` | Prebuilt releases from [bogdanfinn/tls-client](https://github.com/bogdanfinn/tls-client) |
| `libtls_client_jni.*` | Compiled from `jni/tls_client_jni.c` in this repo |

The current bundled version is recorded in [`natives-version.txt`](../natives-version.txt).

---

## Automatic updates (CI)

The repo includes a daily GitHub Actions pipeline (`.github/workflows/update-natives.yml`) that runs at **3:00 UTC** and:

1. Checks if `bogdanfinn/tls-client` has published a new release
2. If yes — downloads the new prebuilt Go libs for all desktop platforms, rebuilds Android libs using NDK on CI, rebuilds macOS JNI bridges
3. Commits the updated files, bumps the minor version (`v1.0.x` → `v1.1.0`), and pushes the tag
4. The normal CI pipeline then builds and publishes a new GitHub Release automatically

You don't need to do anything — updates happen on their own.

To trigger it manually: **GitHub → Actions → Update Native Libraries → Run workflow**

---

## Supported platforms

| Directory | Platform | Go lib | JNI bridge |
|---|---|---|---|
| `arm64-v8a` | Android ARM64 | Built on CI (Android NDK) | Built on CI (Android NDK) |
| `armeabi-v7a` | Android ARM32 | Built on CI (Android NDK) | Built on CI (Android NDK) |
| `linux-x86_64` | Linux x86_64 | Prebuilt from bogdanfinn | Cross-compiled (gcc) |
| `linux-aarch64` | Linux ARM64 | Prebuilt from bogdanfinn | Cross-compiled (aarch64-linux-gnu-gcc) |
| `macos-arm64` | macOS Apple Silicon | Prebuilt from bogdanfinn | Built on macOS CI runner |
| `macos-x86_64` | macOS Intel | Prebuilt from bogdanfinn | Cross-compiled on macOS runner (-arch x86_64) |
| `windows-x86_64` | Windows x64 | Prebuilt from bogdanfinn | Cross-compiled (mingw-w64) |

---

## Manual rebuild (if needed)

### Android

Requires WSL + Android NDK installed via Android Studio. Run:

```bash
bash build_android.sh
```

See the script itself for NDK detection and options.

### Desktop (Linux, Windows)

Requires WSL with `gcc`, `gcc-aarch64-linux-gnu`, `gcc-mingw-w64-x86-64` installed.
Downloads prebuilt Go libs from bogdanfinn and cross-compiles the JNI bridges.

```bash
bash build_desktop.sh
```

### macOS JNI bridges

Requires a macOS machine with Xcode command line tools and JDK 21.

```bash
clang -dynamiclib -arch arm64 -o libtls_client_jni_arm64.dylib \
  jni/tls_client_jni.c \
  -I$JAVA_HOME/include -I$JAVA_HOME/include/darwin

clang -dynamiclib -arch x86_64 -o libtls_client_jni_x86_64.dylib \
  jni/tls_client_jni.c \
  -I$JAVA_HOME/include -I$JAVA_HOME/include/darwin
```

Then copy to `src/main/resources/dev/kotlintls/natives/macos-{arm64,x86_64}/libtls_client_jni.dylib`.

---

## After rebuilding

Rebuild the JAR to include the updated libraries:

```bash
./gradlew jar
```

Or push a new tag to have CI build and release it automatically:

```bash
git tag v1.x.x && git push origin v1.x.x
```
