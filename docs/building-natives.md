# Building the Native Libraries

The native `.so` files bundled in the JAR are built from two sources:

| File | Source |
|---|---|
| `libtls_client_go.so` | Go [bogdanfinn/tls-client](https://github.com/bogdanfinn/tls-client) — the TLS fingerprinting engine |
| `libtls_client_jni.so` | `jni/tls_client_jni.c` in this repo — JNI bridge between Kotlin and Go |

Both are cross-compiled for Android ARM using the Android NDK.

---

## Prerequisites

You need **WSL** (Windows Subsystem for Linux) running on Windows.

### 1. WSL

Install WSL from PowerShell (admin):
```powershell
wsl --install
```
Restart, then open the WSL terminal (Ubuntu by default).

### 2. Go (inside WSL)

```bash
# Download and install Go
wget https://go.dev/dl/go1.23.0.linux-amd64.tar.gz
sudo tar -C /usr/local -xzf go1.23.0.linux-amd64.tar.gz

# Add to PATH (add this line to ~/.bashrc too)
export PATH=$PATH:/usr/local/go/bin

# Verify
go version
```

### 3. Android NDK (via Android Studio on Windows)

1. Open **Android Studio**
2. Go to **Settings → SDK Manager → SDK Tools**
3. Check **NDK (Side by side)** and click **OK**

The NDK will be installed to:
```
C:\Users\<YourName>\AppData\Local\Android\Sdk\ndk\<version>\
```

The build script auto-detects this path — you don't need to set anything manually.

### 4. `git` (inside WSL)

```bash
sudo apt-get update && sudo apt-get install -y git
```

---

## Running the Build

The repo includes `build_android.sh` which does everything automatically.

### Step 1 — Fix line endings (Windows only, run once)

The script must have Unix line endings (LF, not CRLF). Run this in PowerShell:

```powershell
$f = "C:\Coding\kotlin-tls-client\build_android.sh"
$content = [System.IO.File]::ReadAllText($f)
$content = $content -replace "`r`n", "`n"
[System.IO.File]::WriteAllText($f, $content)
```

### Step 2 — Run the script from WSL

Open a WSL terminal as your user and run:

```bash
bash /mnt/c/Coding/kotlin-tls-client/build_android.sh
```

Or if you have a custom NDK location:

```bash
NDK=/mnt/c/Users/YourName/AppData/Local/Android/Sdk/ndk/28.0.0 \
  bash /mnt/c/Coding/kotlin-tls-client/build_android.sh
```

### Step 3 — Rebuild the JAR

Back in Windows PowerShell:

```powershell
cd C:\Coding\kotlin-tls-client
.\gradlew jar
```

The final JAR at `build/libs/kotlin-tls-client-1.0.0.jar` now contains the fresh `.so` files.

---

## What the Script Does

1. **Clones** `bogdanfinn/tls-client` into `/tmp/tls-build/`
2. **Builds** `libtls_client_go.so` for `arm64-v8a` and `armeabi-v7a` using `go build -buildmode=c-shared` with CGO and the Android NDK clang compiler
3. **Compiles** `jni/tls_client_jni.c` into `libtls_client_jni.so` for both ABIs using the same NDK clang
4. **Copies** all four `.so` files to `src/main/resources/dev/kotlintls/natives/{abi}/`

The output structure:
```
src/main/resources/dev/kotlintls/natives/
├── arm64-v8a/
│   ├── libtls_client_go.so   (~14 MB)
│   └── libtls_client_jni.so  (~50 KB)
└── armeabi-v7a/
    ├── libtls_client_go.so   (~14 MB)
    └── libtls_client_jni.so  (~50 KB)
```

These get bundled into the JAR and are automatically extracted at runtime on Android.

---

## Troubleshooting

**`go: command not found`**
Go is installed but not in the non-interactive shell PATH. Make sure `/usr/local/go/bin` is in `~/.bashrc`.

**`NDK not found`**
Install the NDK via Android Studio SDK Manager, or set the `NDK` variable manually:
```bash
NDK=/path/to/ndk/28.x.x bash build_android.sh
```

**`\r': command not found`**
The script has Windows line endings (CRLF). Run the line-ending fix in Step 1 above.

**`-buildmode=c-shared requires exactly one main package`**
Make sure the script is building from the `cffi_dist` subdirectory inside the cloned repo, not from the repo root. The current `build_android.sh` handles this correctly with `cd tls-client/cffi_dist`.

**JNI compile fails with `no such file`**
The Windows NDK `clang.exe` cannot read Linux paths (`/mnt/c/...`). The script converts paths using `wslpath -w` to produce Windows-style paths (`C:\...`) before passing them to clang.
