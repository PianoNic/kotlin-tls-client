# Replace JNI Bridge with JNA + Fork tls-client for Auto-Build

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate the C JNI bridge by switching to JNA (direct calls to Go shared libs), and fork bogdanfinn/tls-client with CI that auto-builds all platform binaries when upstream releases new versions.

**Architecture:** Replace `NativeTlsEngine`'s JNI `external fun` declarations with a JNA `interface` that directly calls the 4 exported Go functions (`request`, `destroySession`, `getCookiesFromSession`, `destroyAll`). Remove all `libtls_client_jni.*` binaries and the C source. The fork of bogdanfinn/tls-client will have a GitHub Actions workflow that syncs upstream, builds Go binaries for all 7 desktop/Android targets, and publishes them as releases. `update-natives.yml` in kotlin-tls-client will pull from the fork instead of upstream.

**Tech Stack:** JNA 5.x, Kotlin/JVM 21, GitHub Actions, Go cross-compilation, Android NDK

---

## File Structure

### kotlin-tls-client changes

| Action | File | Purpose |
|--------|------|---------|
| Create | `src/main/kotlin/dev/kotlintls/GoTlsClient.kt` | JNA interface + library loading |
| Modify | `src/main/kotlin/dev/kotlintls/NativeTlsEngine.kt` | Replace JNI externals with JNA calls |
| Modify | `src/main/kotlin/dev/kotlintls/NativeLibLoader.kt` | Remove JNI bridge loading, only load Go lib |
| Modify | `build.gradle.kts` | Add JNA dependency |
| Modify | `.github/workflows/update-natives.yml` | Pull from fork, remove JNI build steps |
| Delete | `jni/tls_client_jni.c` | No longer needed |
| Delete | `build_android.sh` | Fork CI handles this |
| Delete | All `*tls_client_jni*` files under `src/main/resources/dev/kotlintls/natives/` | No longer needed |

### Fork: PianoNic/tls-client

| Action | File | Purpose |
|--------|------|---------|
| Create | `.github/workflows/sync-and-build.yml` | Auto-sync upstream + build all platforms |

---

### Task 1: Add JNA dependency

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Add JNA to dependencies**

```kotlin
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("net.java.dev.jna:jna:5.16.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.22")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```
git add build.gradle.kts
git commit -m "Add JNA dependency"
```

---

### Task 2: Create JNA interface for Go tls-client

**Files:**
- Create: `src/main/kotlin/dev/kotlintls/GoTlsClient.kt`

- [ ] **Step 1: Create the JNA interface**

```kotlin
package dev.kotlintls

import com.sun.jna.Library
import com.sun.jna.Native

/**
 * JNA interface to the Go tls-client shared library.
 * All 4 functions take/return C strings (JSON).
 */
internal interface GoTlsClient : Library {
    fun request(payload: String): String
    fun destroySession(payload: String): String
    fun getCookiesFromSession(payload: String): String
    fun destroyAll(): String

    companion object {
        fun load(libPath: String): GoTlsClient =
            Native.load(libPath, GoTlsClient::class.java) as GoTlsClient

        fun loadSystem(libName: String): GoTlsClient =
            Native.load(libName, GoTlsClient::class.java) as GoTlsClient
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```
git add src/main/kotlin/dev/kotlintls/GoTlsClient.kt
git commit -m "Add JNA interface for Go tls-client"
```

---

### Task 3: Rewrite NativeLibLoader to only load the Go library via JNA

**Files:**
- Modify: `src/main/kotlin/dev/kotlintls/NativeLibLoader.kt`

- [ ] **Step 1: Rewrite NativeLibLoader**

Replace the entire file contents with:

```kotlin
package dev.kotlintls

import java.io.File

/**
 * Extracts the bundled Go tls-client shared library from JAR resources and loads it via JNA.
 * No JNI bridge needed — JNA calls the Go exports directly.
 *
 * Supports Linux (x86_64, aarch64), macOS (arm64, x86_64), Windows (x86_64).
 * On Android or unsupported platforms, falls back to System.loadLibrary().
 */
internal object NativeLibLoader {

    @Volatile
    internal var instance: GoTlsClient? = null

    @Synchronized
    fun ensureLoaded(): GoTlsClient {
        instance?.let { return it }

        val isAndroid = try { Class.forName("android.os.Build"); true } catch (_: ClassNotFoundException) { false }
        val platform = detectPlatform()

        if (platform != null && !isAndroid) {
            val goPath = extract(
                "dev/kotlintls/natives/${platform.dir}/${platform.goLib}",
                "tls_client_go",
                platform.ext
            )
            if (goPath != null) {
                val lib = GoTlsClient.load(goPath)
                instance = lib
                return lib
            }
        }

        // Android or fallback: load from system path
        val lib = GoTlsClient.loadSystem("tls_client_go")
        instance = lib
        return lib
    }

    private data class Platform(val dir: String, val ext: String) {
        val goLib get() = if (ext == "dll") "tls_client_go.$ext" else "libtls_client_go.$ext"
    }

    private fun detectPlatform(): Platform? {
        val arch = System.getProperty("os.arch") ?: return null
        val os = System.getProperty("os.name")?.lowercase() ?: return null
        val isAndroid = try { Class.forName("android.os.Build"); true } catch (_: ClassNotFoundException) { false }

        return when {
            isAndroid && arch == "aarch64"             -> Platform("arm64-v8a", "so")
            isAndroid && arch in ARM32                  -> Platform("armeabi-v7a", "so")
            os.contains("linux") && isAmd64(arch)      -> Platform("linux-x86_64", "so")
            os.contains("linux") && arch == "aarch64"  -> Platform("linux-aarch64", "so")
            os.contains("mac") && arch == "aarch64"    -> Platform("macos-arm64", "dylib")
            os.contains("mac") && isAmd64(arch)        -> Platform("macos-x86_64", "dylib")
            os.contains("windows") && isAmd64(arch)    -> Platform("windows-x86_64", "dll")
            else -> null
        }
    }

    private val ARM32 = setOf("arm", "armv7l")
    private fun isAmd64(arch: String) = arch == "amd64" || arch == "x86_64"

    private fun extract(resourcePath: String, libName: String, ext: String): String? {
        val stream = NativeLibLoader::class.java.classLoader
            ?.getResourceAsStream(resourcePath) ?: return null

        val tmpDir = File(System.getProperty("java.io.tmpdir"), "kotlintls-natives")
        tmpDir.mkdirs()
        val out = File(tmpDir, "$libName.$ext")

        if (!out.exists()) {
            stream.use { it.copyTo(out.outputStream()) }
            out.setExecutable(true)
            out.setReadable(true)
        }

        return out.absolutePath
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```
git add src/main/kotlin/dev/kotlintls/NativeLibLoader.kt
git commit -m "Rewrite NativeLibLoader to use JNA instead of JNI"
```

---

### Task 4: Rewrite NativeTlsEngine to use JNA

**Files:**
- Modify: `src/main/kotlin/dev/kotlintls/NativeTlsEngine.kt`

- [ ] **Step 1: Replace JNI externals with JNA calls**

Replace the entire file contents with:

```kotlin
package dev.kotlintls

/**
 * Uses the Go tls-client library so your requests look like a real browser (Chrome, Firefox, etc.).
 *
 * The native Go library is bundled inside the JAR and loaded automatically via JNA on first use.
 * No JNI bridge or manual setup required — just use TlsClient(NativeTlsEngine()).
 *
 * Supported platforms: Linux (x86_64, aarch64), macOS (arm64, x86_64), Windows (x86_64).
 * Android: arm64-v8a, armeabi-v7a (via System.loadLibrary).
 */
class NativeTlsEngine : TlsClientEngine {

    private val lib: GoTlsClient = NativeLibLoader.ensureLoaded()

    override fun request(requestJson: String): String = lib.request(requestJson)
    override fun destroySession(payloadJson: String): String = lib.destroySession(payloadJson)
    override fun getCookiesFromSession(payloadJson: String): String = lib.getCookiesFromSession(payloadJson)
    override fun destroyAll(): String = lib.destroyAll()
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run tests**

Run: `./gradlew test`
Expected: All existing tests pass (tests use `TlsClient()` without engine, so no native lib needed)

- [ ] **Step 4: Commit**

```
git add src/main/kotlin/dev/kotlintls/NativeTlsEngine.kt
git commit -m "Replace JNI external functions with JNA calls"
```

---

### Task 5: Delete JNI bridge and all JNI binaries

**Files:**
- Delete: `jni/tls_client_jni.c`
- Delete: `build_android.sh`
- Delete: All `*tls_client_jni*` files in `src/main/resources/dev/kotlintls/natives/`

- [ ] **Step 1: Delete C source and build script**

```bash
rm jni/tls_client_jni.c
rmdir jni
rm build_android.sh
```

- [ ] **Step 2: Delete all JNI bridge binaries**

```bash
rm src/main/resources/dev/kotlintls/natives/arm64-v8a/libtls_client_jni.so
rm src/main/resources/dev/kotlintls/natives/armeabi-v7a/libtls_client_jni.so
rm src/main/resources/dev/kotlintls/natives/linux-aarch64/libtls_client_jni.so
rm src/main/resources/dev/kotlintls/natives/linux-x86_64/libtls_client_jni.so
rm src/main/resources/dev/kotlintls/natives/macos-arm64/libtls_client_jni.dylib
rm src/main/resources/dev/kotlintls/natives/macos-x86_64/libtls_client_jni.dylib
rm src/main/resources/dev/kotlintls/natives/windows-x86_64/tls_client_jni.dll
```

- [ ] **Step 3: Verify it still compiles and tests pass**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 4: Commit**

```
git add -A
git commit -m "Remove JNI C bridge and all JNI binaries"
```

---

### Task 6: Fork bogdanfinn/tls-client and add auto-build CI

**Files:**
- Create: `.github/workflows/sync-and-build.yml` (in the fork)

- [ ] **Step 1: Fork the repo**

```bash
gh repo fork bogdanfinn/tls-client --clone=false --remote=false
```

This creates `PianoNic/tls-client` on GitHub.

- [ ] **Step 2: Clone the fork locally**

```bash
cd /c/GithubProjects
git clone https://github.com/PianoNic/tls-client.git tls-client-fork
cd tls-client-fork
```

- [ ] **Step 3: Create the auto-sync + build workflow**

Create `.github/workflows/sync-and-build.yml`:

```yaml
name: Sync Upstream and Build

on:
  schedule:
    - cron: '0 4 * * *'   # daily at 4:00 UTC
  workflow_dispatch:

permissions:
  contents: write

jobs:

  sync:
    runs-on: ubuntu-latest
    outputs:
      new_tag: ${{ steps.check.outputs.new_tag }}
      version: ${{ steps.check.outputs.version }}
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Sync with upstream
        run: |
          git remote add upstream https://github.com/bogdanfinn/tls-client.git || true
          git fetch upstream --tags
          git merge upstream/master --no-edit || true
          git push origin main --tags

      - name: Check for new release tag
        id: check
        run: |
          LATEST=$(git tag --sort=-v:refname | head -1)
          BUILT=$(gh release list --limit 1 --json tagName --jq '.[0].tagName' 2>/dev/null || echo "none")
          if [ "$LATEST" != "$BUILT" ]; then
            echo "new_tag=true" >> $GITHUB_OUTPUT
            echo "version=${LATEST#v}" >> $GITHUB_OUTPUT
            echo "New tag to build: $LATEST (last built: $BUILT)"
          else
            echo "new_tag=false" >> $GITHUB_OUTPUT
            echo "Already up to date: $BUILT"
          fi
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

  build-linux-windows:
    needs: sync
    if: needs.sync.outputs.new_tag == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - uses: actions/setup-go@v5
        with: { go-version: '1.23' }
      - name: Build
        working-directory: cffi_dist
        run: |
          # Linux amd64
          CGO_ENABLED=1 GOOS=linux GOARCH=amd64 \
            go build -buildmode=c-shared -o ../out/linux-x86_64/libtls_client_go.so .
          # Linux arm64
          sudo apt-get install -y gcc-aarch64-linux-gnu
          CC=aarch64-linux-gnu-gcc CGO_ENABLED=1 GOOS=linux GOARCH=arm64 \
            go build -buildmode=c-shared -o ../out/linux-aarch64/libtls_client_go.so .
          # Windows amd64
          sudo apt-get install -y gcc-mingw-w64-x86-64
          CC=x86_64-w64-mingw32-gcc CGO_ENABLED=1 GOOS=windows GOARCH=amd64 \
            go build -buildmode=c-shared -o ../out/windows-x86_64/tls_client_go.dll .
      - uses: actions/upload-artifact@v4
        with: { name: linux-windows, path: out/ }

  build-macos:
    needs: sync
    if: needs.sync.outputs.new_tag == 'true'
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - uses: actions/setup-go@v5
        with: { go-version: '1.23' }
      - name: Build
        working-directory: cffi_dist
        run: |
          CGO_ENABLED=1 GOOS=darwin GOARCH=arm64 \
            go build -buildmode=c-shared -o ../out/macos-arm64/libtls_client_go.dylib .
          CGO_ENABLED=1 GOOS=darwin GOARCH=amd64 \
            go build -buildmode=c-shared -o ../out/macos-x86_64/libtls_client_go.dylib .
      - uses: actions/upload-artifact@v4
        with: { name: macos, path: out/ }

  build-android:
    needs: sync
    if: needs.sync.outputs.new_tag == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - uses: actions/setup-go@v5
        with: { go-version: '1.23' }
      - name: Build
        working-directory: cffi_dist
        run: |
          NDK_BIN=$(find "$ANDROID_HOME/ndk" -name "aarch64-linux-android21-clang" | head -1 | xargs dirname)
          CC="$NDK_BIN/aarch64-linux-android21-clang" \
            CGO_ENABLED=1 GOOS=android GOARCH=arm64 \
            go build -buildmode=c-shared -o ../out/arm64-v8a/libtls_client_go.so .
          CC="$NDK_BIN/armv7a-linux-androideabi21-clang" \
            CGO_ENABLED=1 GOOS=android GOARCH=arm \
            go build -buildmode=c-shared -o ../out/armeabi-v7a/libtls_client_go.so .
      - uses: actions/upload-artifact@v4
        with: { name: android, path: out/ }

  release:
    needs: [sync, build-linux-windows, build-macos, build-android]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/download-artifact@v4
        with: { name: linux-windows, path: out/ }
      - uses: actions/download-artifact@v4
        with: { name: macos, path: out/ }
      - uses: actions/download-artifact@v4
        with: { name: android, path: out/ }

      - name: Create release
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          VERSION="${{ needs.sync.outputs.version }}"
          TAG="v$VERSION"

          # Zip each platform
          cd out
          for dir in */; do
            platform="${dir%/}"
            zip -j "../$platform.zip" "$dir"/*
          done
          cd ..

          gh release create "$TAG" \
            *.zip \
            --title "tls-client $TAG" \
            --notes "Auto-built from upstream bogdanfinn/tls-client $TAG"
```

- [ ] **Step 4: Push the workflow**

```bash
git add .github/workflows/sync-and-build.yml
git commit -m "Add auto-sync and build workflow"
git push origin main
```

- [ ] **Step 5: Verify the workflow appears on GitHub**

```bash
gh workflow list --repo PianoNic/tls-client
```

Expected: `Sync Upstream and Build` listed

- [ ] **Step 6: Trigger a manual run to test**

```bash
gh workflow run sync-and-build.yml --repo PianoNic/tls-client
```

---

### Task 7: Update kotlin-tls-client CI to pull from fork

**Files:**
- Modify: `.github/workflows/update-natives.yml`

- [ ] **Step 1: Rewrite update-natives.yml**

Replace the entire file with:

```yaml
name: Update Native Libraries

on:
  schedule:
    - cron: '0 5 * * *'   # daily at 5:00 UTC (1h after fork builds)
  workflow_dispatch:

permissions:
  contents: write

jobs:

  check:
    runs-on: ubuntu-latest
    outputs:
      update: ${{ steps.check.outputs.update }}
      version: ${{ steps.check.outputs.version }}
    steps:
      - uses: actions/checkout@v4
      - id: check
        run: |
          LATEST=$(curl -s https://api.github.com/repos/PianoNic/tls-client/releases/latest \
            | jq -r '.tag_name' | sed 's/^v//')
          CURRENT=$(cat natives-version.txt 2>/dev/null || echo "none")
          echo "version=$LATEST" >> $GITHUB_OUTPUT
          if [ "$LATEST" != "$CURRENT" ]; then
            echo "update=true"  >> $GITHUB_OUTPUT
          else
            echo "update=false" >> $GITHUB_OUTPUT
          fi
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

  update:
    needs: check
    if: needs.check.outputs.update == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }

      - name: Download binaries from fork
        run: |
          VERSION="${{ needs.check.outputs.version }}"
          TAG="v$VERSION"
          BASE="https://github.com/PianoNic/tls-client/releases/download/$TAG"

          dl() { curl -fL --retry 3 -o "$1" "$2"; }
          mkdir -p /tmp/natives

          for platform in linux-x86_64 linux-aarch64 windows-x86_64 macos-arm64 macos-x86_64 arm64-v8a armeabi-v7a; do
            dl "/tmp/natives/$platform.zip" "$BASE/$platform.zip"
            mkdir -p "src/main/resources/dev/kotlintls/natives/$platform"
            unzip -o "/tmp/natives/$platform.zip" -d "src/main/resources/dev/kotlintls/natives/$platform/"
          done

          echo "$VERSION" > natives-version.txt

      - name: Commit, bump minor version, push tag
        run: |
          git config user.name "PianoNic"
          git config user.email "79938743+PianoNic@users.noreply.github.com"

          git add -A
          git commit -m "Update native libraries to tls-client v${{ needs.check.outputs.version }} [skip ci]"
          git push

          CURRENT=$(git describe --tags --abbrev=0)
          MAJOR=$(echo "${CURRENT#v}" | cut -d. -f1)
          MINOR=$(echo "${CURRENT#v}" | cut -d. -f2)
          NEW_TAG="v$MAJOR.$((MINOR + 1)).0"

          git tag "$NEW_TAG"
          git push origin "$NEW_TAG"
```

- [ ] **Step 2: Commit**

```
git add .github/workflows/update-natives.yml
git commit -m "Pull native binaries from fork instead of upstream"
```

---

### Task 8: Clean up docs referencing JNI

**Files:**
- Modify: `docs/building-natives.md` (update or remove JNI references)
- Modify: `docs/api/native-engine.md` (update to mention JNA)
- Modify: `CLAUDE.md` (update architecture section)

- [ ] **Step 1: Update CLAUDE.md architecture section**

Replace the `### Key layers` section's NativeTlsEngine bullet:

Old:
```
- **`NativeTlsEngine`** — JNI bridge to the Go `tls-client` shared library. Handles JSON serialization of requests/responses.
```

New:
```
- **`NativeTlsEngine`** — calls the Go `tls-client` shared library directly via JNA. No C bridge needed.
- **`GoTlsClient`** — JNA interface declaring the 4 Go exports (`request`, `destroySession`, `getCookiesFromSession`, `destroyAll`).
```

Remove from CLAUDE.md architecture:
```
- JNI bridges built in CI for each platform
```

- [ ] **Step 2: Update docs/api/native-engine.md** to remove JNI references and mention JNA

- [ ] **Step 3: Update docs/building-natives.md** to remove JNI build instructions

- [ ] **Step 4: Commit**

```
git add -A
git commit -m "Update docs to reflect JNA migration"
```
