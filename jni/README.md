# How to get real browser fingerprint (JA3) in your app

This folder has the small C “bridge” so your Kotlin/Android app can use the **Go tls-client** and get the **same TLS fingerprint** as Chrome or Firefox.

---

## What you need to do (simple steps)

### Step 1: Build the Go library for your device

From the **Go tls-client** project you build one file: a **library** (on Android it’s called something like `libtls_client_go.so`).

- On **PC** (Windows/Mac/Linux): build it so you get a `.so` / `.dll` / `.dylib` that has the same C functions the Node/Python clients use.
- On **Android**: build one library per CPU type (e.g. arm64, arm, x86). Put each file in your app’s `jniLibs` folder (e.g. `jniLibs/arm64-v8a/libtls_client_go.so`).

So: **one (or a few) file(s) from the Go project** that your app will load.

### Step 2: Build the small C bridge (this folder)

We give you the C code in `tls_client_jni.c`. You compile it into **another** library (e.g. `libtls_client_jni.so`).

- On **Android**: use the NDK to compile `tls_client_jni.c` and put the result in the same `jniLibs` folders as the Go library.
- On **PC**: compile the same C file for your OS and put the result where your app can load it.

So: **one more library** that “connects” Kotlin to the Go library.

### Step 3: Load both in your app

When your app starts (e.g. in `Application.onCreate` on Android), load the libraries **in this order**:

```kotlin
System.loadLibrary("tls_client_go")   // Go library first
System.loadLibrary("tls_client_jni") // then our bridge
```

### Step 4: Use the client with the “native” engine

Create the client like this:

```kotlin
val client = TlsClient(NativeTlsEngine())
```

Then use `client.request(...)` and the rest of the API as usual. The request is done by the **Go** code, so the site sees a **real browser** fingerprint (JA3).

---

## What the Go library must “export”

The Go tls-client build must expose these names to C:

- `request` — send a request (JSON in, JSON out)
- `destroySession` — close a session (JSON in, JSON out)
- `getCookiesFromSession` — get cookies for a URL (JSON in, JSON out)
- `destroyAll` — close all sessions (no input, JSON out)

The JSON format is the same as in the Go project’s FFI (RequestInput, Response, etc.). Our Kotlin side already talks that format.

---

That’s it. Build the Go library → build this C bridge → load both → use `TlsClient(NativeTlsEngine())` for real JA3.
