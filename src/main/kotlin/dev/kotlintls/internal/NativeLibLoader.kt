package dev.kotlintls.internal

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
                val lib = loadWithWindowsAvRetry(goPath)
                instance = lib
                return lib
            }
        }

        // Android or fallback: load from system path
        val lib = GoTlsClient.loadSystem("tls_client_go")
        instance = lib
        return lib
    }

    /**
     * Loading a freshly-extracted DLL on Windows can race with the OS-level virus scan
     * (Defender / 3rd-party AV) holding the file briefly. Retry a few times with a short
     * backoff before giving up.
     */
    private fun loadWithWindowsAvRetry(libPath: String): GoTlsClient {
        // Give Windows Defender / AV a head-start to finish scanning a freshly-written
        // multi-MB DLL before we try to load it.
        Thread.sleep(500)
        var lastError: Throwable? = null
        for (attempt in 1..15) {
            try {
                return GoTlsClient.load(libPath)
            } catch (e: UnsatisfiedLinkError) {
                lastError = e
                Thread.sleep(500)
            }
        }
        throw lastError ?: IllegalStateException("Failed to load $libPath")
    }

    private data class Platform(val dir: String, val ext: String) {
        val goLib get() = if (ext == "dll") "tls_client_go.$ext" else "libtls_client_go.$ext"
    }

    private fun detectPlatform(): Platform? {
        val arch = System.getProperty("os.arch") ?: return null
        val os = System.getProperty("os.name")?.lowercase() ?: return null
        val isAndroid = try { Class.forName("android.os.Build"); true } catch (_: ClassNotFoundException) { false }

        return when {
            isAndroid && arch == "aarch64"              -> Platform("android-arm64-v8a", "so")
            isAndroid && arch in ARM32                   -> Platform("android-armeabi-v7a", "so")
            isAndroid && arch in X86_32                  -> Platform("android-x86", "so")
            isAndroid && isAmd64(arch)                   -> Platform("android-x86_64", "so")
            os.contains("linux") && isAmd64(arch)       -> Platform("linux-x86_64", "so")
            os.contains("linux") && arch == "aarch64"   -> Platform("linux-aarch64", "so")
            os.contains("linux") && arch in ARM32        -> Platform("linux-arm", "so")
            os.contains("mac") && arch == "aarch64"     -> Platform("macos-arm64", "dylib")
            os.contains("mac") && isAmd64(arch)         -> Platform("macos-x86_64", "dylib")
            os.contains("windows") && isAmd64(arch)     -> Platform("windows-x86_64", "dll")
            os.contains("windows") && arch == "aarch64" -> Platform("windows-arm64", "dll")
            os.contains("freebsd") && isAmd64(arch)     -> Platform("freebsd-x86_64", "so")
            else -> null
        }
    }

    private val ARM32 = setOf("arm", "armv7l")
    private val X86_32 = setOf("x86", "i386", "i686")
    private fun isAmd64(arch: String) = arch == "amd64" || arch == "x86_64"

    private fun extract(resourcePath: String, libName: String, ext: String): String? {
        val stream = NativeLibLoader::class.java.classLoader
            ?.getResourceAsStream(resourcePath) ?: return null

        val tmpDir = File(System.getProperty("java.io.tmpdir"), "kotlintls-natives")
        tmpDir.mkdirs()

        // Per-JVM unique filename. Avoids:
        //  - stale extracts from older library versions reused after upgrade
        //  - "file in use by another process" errors on Windows when AV / parallel
        //    JVMs hold a lock on a shared cached path.
        val pid = ProcessHandle.current().pid()
        val unique = "${libName}_${pid}_${System.nanoTime()}.$ext"
        val out = File(tmpDir, unique)

        // Close BOTH streams: the input via `use`, and the output via a nested `use`.
        // Leaving the output stream open holds a write-share lock that prevents the
        // JVM itself from later loading the DLL ("file in use by another process").
        stream.use { input ->
            out.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        out.setExecutable(true)
        out.setReadable(true)
        out.deleteOnExit()

        return out.absolutePath
    }
}
