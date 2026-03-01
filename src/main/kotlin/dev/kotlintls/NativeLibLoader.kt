package dev.kotlintls

import java.io.File

/**
 * Extracts the bundled native libraries from the JAR resources and loads them.
 * Supports Android (arm64-v8a, armeabi-v7a), Linux (x86_64, aarch64),
 * macOS (arm64, x86_64), and Windows (x86_64).
 * Falls back to System.loadLibrary if no bundled lib matches the current platform.
 */
internal object NativeLibLoader {

    @Volatile private var loaded = false

    @Synchronized
    fun ensureLoaded() {
        if (loaded) return

        val platform = detectPlatform()
        if (platform != null) {
            val goPath  = extract("dev/kotlintls/natives/${platform.dir}/${platform.goLib}",  "tls_client_go",  platform.ext)
            val jniPath = extract("dev/kotlintls/natives/${platform.dir}/${platform.jniLib}", "tls_client_jni", platform.ext)
            if (goPath != null && jniPath != null) {
                System.load(goPath)
                System.load(jniPath)
                loaded = true
                return
            }
        }

        // Bundled lib not found for this platform — try pre-installed system libraries
        try { System.loadLibrary("tls_client_go") } catch (_: Throwable) {}
        System.loadLibrary("tls_client_jni")
        loaded = true
    }

    private data class Platform(val dir: String, val ext: String) {
        val goLib  get() = if (ext == "dll") "tls_client_go.$ext"  else "libtls_client_go.$ext"
        val jniLib get() = if (ext == "dll") "tls_client_jni.$ext" else "libtls_client_jni.$ext"
    }

    private fun detectPlatform(): Platform? {
        val arch      = System.getProperty("os.arch")  ?: return null
        val os        = System.getProperty("os.name")?.lowercase() ?: return null
        val isAndroid = try { Class.forName("android.os.Build"); true } catch (_: ClassNotFoundException) { false }

        return when {
            isAndroid && arch == "aarch64"            -> Platform("arm64-v8a",      "so")
            isAndroid && arch in ARM32                -> Platform("armeabi-v7a",    "so")
            os.contains("linux") && isAmd64(arch)    -> Platform("linux-x86_64",   "so")
            os.contains("linux") && arch == "aarch64" -> Platform("linux-aarch64",  "so")
            os.contains("mac")   && arch == "aarch64" -> Platform("macos-arm64",    "dylib")
            os.contains("mac")   && isAmd64(arch)    -> Platform("macos-x86_64",   "dylib")
            os.contains("windows") && isAmd64(arch)  -> Platform("windows-x86_64", "dll")
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
