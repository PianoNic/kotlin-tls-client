#!/bin/bash
# build_android.sh — Build libtls_client_go.so and libtls_client_jni.so for Android
#
# Usage (from WSL on Windows):
#   bash build_android.sh
#
# Or with a custom NDK path:
#   NDK=/path/to/ndk bash build_android.sh
#
# Requirements:
#   - Go installed in WSL (go version)
#   - Android NDK installed (via Android Studio → SDK Manager → SDK Tools → NDK)
#
# Output:
#   src/main/resources/dev/kotlintls/natives/arm64-v8a/   libtls_client_go.so + libtls_client_jni.so
#   src/main/resources/dev/kotlintls/natives/armeabi-v7a/  libtls_client_go.so + libtls_client_jni.so

set -e

# Load user profile so Go and other tools are in PATH
source /etc/profile 2>/dev/null || true
source ~/.bashrc 2>/dev/null || true
source ~/.profile 2>/dev/null || true
export PATH=$PATH:/usr/local/go/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

echo "=== Checking tools ==="
go version
git version

# ── NDK location ────────────────────────────────────────────────────────────
# Auto-detect NDK from common Windows paths if NDK is not set
if [ -z "$NDK" ]; then
    for user_dir in /mnt/c/Users/*/; do
        candidate="$user_dir/AppData/Local/Android/Sdk/ndk"
        if [ -d "$candidate" ]; then
            # Pick the newest NDK version
            NDK=$(ls -1d "$candidate"/*/ 2>/dev/null | sort -V | tail -1 | sed 's|/$||')
            break
        fi
    done
fi

if [ -z "$NDK" ] || [ ! -d "$NDK" ]; then
    echo "ERROR: Android NDK not found."
    echo "Set the NDK variable: NDK=/path/to/ndk/28.x.x bash build_android.sh"
    exit 1
fi

echo "NDK: $NDK"

# Windows NDK ships shell wrapper scripts (no extension) that call clang.exe via WSL interop
CC_ARM64="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin/aarch64-linux-android21-clang"
CC_ARM32="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin/armv7a-linux-androideabi21-clang"

echo "=== NDK check ==="
ls "$CC_ARM64" && echo "arm64 clang OK"
ls "$CC_ARM32" && echo "arm32 clang OK"

# ── Paths ────────────────────────────────────────────────────────────────────
# Detect the repo root from script location (works even if called from another dir)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JNI_C="$SCRIPT_DIR/jni/tls_client_jni.c"
OUTDIR="$SCRIPT_DIR/src/main/resources/dev/kotlintls/natives"
BUILDDIR="/tmp/tls-build"

mkdir -p "$BUILDDIR/arm64" "$BUILDDIR/arm32"
mkdir -p "$OUTDIR/arm64-v8a" "$OUTDIR/armeabi-v7a"

# ── Build Go tls-client ───────────────────────────────────────────────────────
echo "=== Cloning Go tls-client ==="
cd "$BUILDDIR"
rm -rf tls-client
git clone --depth=1 https://github.com/bogdanfinn/tls-client.git
cd tls-client/cffi_dist   # cffi_dist is a separate Go module

echo "=== Building libtls_client_go.so for arm64-v8a ==="
CC="$CC_ARM64" CGO_ENABLED=1 GOOS=android GOARCH=arm64 \
  go build -buildmode=c-shared -o "$BUILDDIR/arm64/libtls_client_go.so" .
echo "arm64 Go lib done"

echo "=== Building libtls_client_go.so for armeabi-v7a ==="
CC="$CC_ARM32" CGO_ENABLED=1 GOOS=android GOARCH=arm GOARM=7 \
  go build -buildmode=c-shared -o "$BUILDDIR/arm32/libtls_client_go.so" .
echo "arm32 Go lib done"

# ── Build JNI bridge ──────────────────────────────────────────────────────────
# clang.exe is a Windows executable — pass Windows-style paths via wslpath
JNI_C_WIN=$(wslpath -w "$JNI_C")

echo "=== Building libtls_client_jni.so for arm64-v8a ==="
OUT_WIN=$(wslpath -w "$OUTDIR/arm64-v8a/libtls_client_jni.so")
"$CC_ARM64" -shared -fPIC "$JNI_C_WIN" -o "$OUT_WIN"
echo "arm64 JNI done"

echo "=== Building libtls_client_jni.so for armeabi-v7a ==="
OUT_WIN=$(wslpath -w "$OUTDIR/armeabi-v7a/libtls_client_jni.so")
"$CC_ARM32" -shared -fPIC "$JNI_C_WIN" -o "$OUT_WIN"
echo "arm32 JNI done"

# ── Copy Go libs to output ────────────────────────────────────────────────────
echo "=== Copying Go libs ==="
cp "$BUILDDIR/arm64/libtls_client_go.so" "$OUTDIR/arm64-v8a/"
cp "$BUILDDIR/arm32/libtls_client_go.so" "$OUTDIR/armeabi-v7a/"

echo ""
echo "=== ALL DONE ==="
ls -lh "$OUTDIR/arm64-v8a/"
ls -lh "$OUTDIR/armeabi-v7a/"
echo ""
echo "Next: run './gradlew jar' to bundle these into the JAR."
