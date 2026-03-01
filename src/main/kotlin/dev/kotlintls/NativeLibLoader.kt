package dev.kotlintls

import java.io.File

/**
 * Extracts the bundled native libraries from the JAR resources and loads them.
 * Supports arm64-v8a and armeabi-v7a (Android). Falls back to System.loadLibrary
 * if no bundled lib is found for the current platform (e.g. user pre-loaded them manually).
 */
internal object NativeLibLoader {

    @Volatile private var loaded = false

    @Synchronized
    fun ensureLoaded() {
        if (loaded) return

        val abi = detectAbi()
        if (abi != null) {
            val goPath = extract("dev/kotlintls/natives/$abi/libtls_client_go.so", "tls_client_go")
            val jniPath = extract("dev/kotlintls/natives/$abi/libtls_client_jni.so", "tls_client_jni")
            if (goPath != null && jniPath != null) {
                System.load(goPath)
                System.load(jniPath)
                loaded = true
                return
            }
        }

        // Bundled libs not found — fall back to pre-installed system libraries
        try { System.loadLibrary("tls_client_go") } catch (_: Throwable) {}
        System.loadLibrary("tls_client_jni")
        loaded = true
    }

    private fun detectAbi(): String? = when (System.getProperty("os.arch")) {
        "aarch64"            -> "arm64-v8a"
        "arm", "armv7l"      -> "armeabi-v7a"
        else                 -> null
    }

    private fun extract(resourcePath: String, libName: String): String? {
        val stream = NativeLibLoader::class.java.classLoader
            ?.getResourceAsStream(resourcePath) ?: return null

        val tmpDir = File(System.getProperty("java.io.tmpdir"), "kotlintls-natives")
        tmpDir.mkdirs()
        val out = File(tmpDir, "$libName.so")

        if (!out.exists()) {
            stream.use { it.copyTo(out.outputStream()) }
            out.setExecutable(true)
            out.setReadable(true)
        }

        return out.absolutePath
    }
}
