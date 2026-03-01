# What you have to do to make everything work

You have two ways to use this library.

---

## Option 1: Use it without real browser fingerprint (simplest)

**You do nothing special.**

- Add the library to your project.
- Use `TlsClient()` (no `NativeTlsEngine`).
- Requests use OkHttp. They work, but the connection does **not** look like Chrome/Firefox to sites that check.

Good for: most APIs, testing, when the site doesn’t care about TLS fingerprint.

---

## Option 2: Use it with real browser fingerprint (like node-tls-client)

Then the connection can look like Chrome/Firefox. You have two sub-options.

### 2a. You ship the native libs (users do nothing)

**You** build the Go tls-client and the JNI wrapper **once** per platform, then ship them with your library so users don’t compile anything.

1. **Build the Go library**  
   From the [Go tls-client](https://github.com/bogdanfinn/tls-client) project, build a shared library for each platform you support, e.g.:
   - Android: `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`
   - (Optional) Desktop: Windows, macOS, Linux

2. **Build the JNI wrapper**  
   Compile `jni/tls_client_jni.c` for the same platforms (e.g. with the Android NDK).

3. **Put the `.so` files where the app can load them**
   - **Android**: Put `libtls_client_go.so` and `libtls_client_jni.so` in your app’s `src/main/jniLibs/<abi>/` (e.g. `jniLibs/arm64-v8a/`). The app will pack them in the APK and `System.loadLibrary("tls_client_go")` / `System.loadLibrary("tls_client_jni")` will find them.
   - **Desktop**: Put the libs in a folder that’s on the library path, or set `java.library.path` when starting the app.

4. **In your app**  
   Load the libs once (e.g. in `Application.onCreate` on Android), then use the client with the native engine:

   ```kotlin
   // Once at startup
   System.loadLibrary("tls_client_go")
   System.loadLibrary("tls_client_jni")

   // Then use it
   val client = TlsClient(NativeTlsEngine())
   client.request(...)
   ```

If **you** are the library author and you want “it just works” for everyone:

- Publish an **Android library** (AAR) that already contains the `.so` files in `jniLibs/`. Then app developers add your library and the two `loadLibrary(...)` lines; they don’t build Go or the JNI wrapper.
- Same idea for other platforms: ship the right native libs with your artifact so users don’t have to compile.

### 2b. Each app developer builds the native libs

If you don’t ship the libs, then **each app** that wants real JA3 must:

1. Build the Go tls-client and the JNI wrapper (see [jni/README.md](../jni/README.md)).
2. Add the resulting `.so` files to the app (e.g. `jniLibs` on Android).
3. Call `loadLibrary("tls_client_go")` and `loadLibrary("tls_client_jni")` at startup, then use `TlsClient(NativeTlsEngine())`.

---

## Summary

| What you want | What you do |
|---------------|-------------|
| No real browser fingerprint, minimal setup | Use `TlsClient()` and the library as-is. Nothing else. |
| Real browser fingerprint, “just work” for users | You build Go + JNI once per platform, put the `.so` in your library/app, load them at startup, use `TlsClient(NativeTlsEngine())`. |
| Real browser fingerprint, you don’t ship libs | Document that app developers must build and add the `.so` files and load them, then use `TlsClient(NativeTlsEngine())`. |

So: **to make everything work with real JA3**, you need the native libraries (Go + JNI) built and included where the app can load them; then the Kotlin side is just those two `loadLibrary` calls and `TlsClient(NativeTlsEngine())`.
