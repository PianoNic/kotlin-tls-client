package dev.kotlintls

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Negative baseline.
 *
 * Hits the same fingerprinting endpoint as [TlsFingerprintTest] but with OkHttp, the most common
 * "naive" HTTP client on the JVM. Every browser-only assertion in [TlsFingerprintTest] is repeated
 * here in inverted form: OkHttp must NOT pass any of them.
 *
 * If any of these tests start failing (i.e. OkHttp suddenly looks like Chrome), that's a real
 * problem worth investigating, but practically it would mean our positive assertions in
 * [TlsFingerprintTest] are tautologies and not catching anything.
 */
class OkHttpNegativeFingerprintTest {

    private val fpUrl = "https://tls.peet.ws/api/all"

    private fun probeWithOkHttp(): JsonObject {
        val client = OkHttpClient.Builder().build()
        val req = Request.Builder().url(fpUrl).build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: error("No body")
            return JsonParser.parseString(body).asJsonObject
        }
    }

    private fun JsonObject.tls(): JsonObject = getAsJsonObject("tls")
    private fun JsonObject.http2OrNull(): JsonObject? =
        if (has("http2") && !get("http2").isJsonNull) getAsJsonObject("http2") else null

    private fun cipherIsGrease(name: String): Boolean {
        if (name.contains("GREASE", ignoreCase = true)) return true
        return Regex("(?i)0x([0-9a-f])a\\1a").containsMatchIn(name)
    }

    @Test
    fun `okhttp first cipher is NOT GREASE`() {
        val fp = probeWithOkHttp()
        val first = fp.tls().getAsJsonArray("ciphers").first().asString
        assertFalse(
            cipherIsGrease(first),
            "OkHttp uses the JDK TLS stack which does not inject GREASE. If '$first' is GREASE, " +
                "either the JDK shipped a behavior change or the test infra is fooling us."
        )
    }

    @Test
    fun `okhttp cipher list contains NO GREASE values`() {
        val fp = probeWithOkHttp()
        val ciphers = fp.tls().getAsJsonArray("ciphers").map { it.asString }
        assertFalse(
            ciphers.any(::cipherIsGrease),
            "OkHttp must produce a GREASE-free cipher list (proves our 'GREASE present' assertion " +
                "for Chrome is meaningful). Got: $ciphers"
        )
    }

    @Test
    fun `okhttp does NOT advertise encrypted client hello`() {
        val fp = probeWithOkHttp()
        val extensions = fp.tls().getAsJsonArray("extensions").map { it.asJsonObject.get("name").asString }
        assertFalse(
            extensions.any { it.contains("65037") },
            "OkHttp must not emit the encrypted_client_hello (65037) extension. " +
                "Got: $extensions"
        )
    }

    @Test
    fun `okhttp does NOT advertise ALPS extension`() {
        val fp = probeWithOkHttp()
        val extensions = fp.tls().getAsJsonArray("extensions").map { it.asJsonObject.get("name").asString }
        assertFalse(
            extensions.any { it.contains("17613") || it.contains("17513") || it.contains("application_settings", ignoreCase = true) },
            "OkHttp must not emit ALPS (17613/17513). Got: $extensions"
        )
    }

    @Test
    fun `okhttp does NOT use brotli for compress_certificate`() {
        val fp = probeWithOkHttp()
        val extensions = fp.tls().getAsJsonArray("extensions").map { it.asJsonObject }
        val compress = extensions.firstOrNull { it.get("name").asString.startsWith("compress_certificate") }
        // Either the extension is absent (most likely) or, if present, doesn't list brotli.
        if (compress != null) {
            val algorithms = compress.getAsJsonArray("algorithms").map { it.asString }
            assertFalse(
                algorithms.any { it.startsWith("brotli", ignoreCase = true) },
                "OkHttp must not list brotli in compress_certificate. Got: $algorithms"
            )
        }
    }

    @Test
    fun `okhttp does NOT advertise X25519MLKEM768`() {
        val fp = probeWithOkHttp()
        val extensions = fp.tls().getAsJsonArray("extensions").map { it.asJsonObject }
        val groupsExt = extensions.firstOrNull { it.get("name").asString.startsWith("supported_groups") }
        if (groupsExt == null) {
            // No supported_groups extension at all is fine, that just means the JDK didn't send it.
            return
        }
        val groups = groupsExt.getAsJsonArray("supported_groups").map { it.asString }
        assertFalse(
            groups.any { it.contains("X25519MLKEM768") || it.contains("MLKEM") },
            "OkHttp must not advertise post-quantum X25519MLKEM768. Got: $groups"
        )
    }

    @Test
    fun `okhttp Akamai HTTP2 fingerprint differs from Chromium fingerprint`() {
        val fp = probeWithOkHttp()
        val http2 = fp.http2OrNull()
        if (http2 == null) {
            // OkHttp negotiated HTTP/1.1 in this run, no Akamai fingerprint to compare. That itself
            // is a non-browser signal (modern Chrome always picks h2 with peet.ws).
            return
        }
        val akamai = http2.get("akamai_fingerprint").asString
        val chromiumAkamai = "1:65536;2:0;4:6291456;6:262144|15663105|0|m,a,s,p"
        assertNotEquals(
            chromiumAkamai, akamai,
            "OkHttp must not produce Chromium's Akamai HTTP/2 fingerprint string."
        )
    }

    @Test
    fun `okhttp JA4 differs from real Chrome 133 JA4`() {
        // Cross-check vs the canonical Chrome 133 JA4 prefix from a real Chrome handshake.
        // Strict equality on the full hash is too brittle (rotates with profile updates), so just
        // assert OkHttp's JA4 has a different leading segment.
        val fp = probeWithOkHttp()
        val ja4 = fp.tls().get("ja4").asString
        // Chrome 133's first segment ends with "h2_..." and starts with "t13d1516h2".
        // OkHttp's first segment differs in cipher count and extension count.
        assertFalse(
            ja4.startsWith("t13d1516h2"),
            "OkHttp JA4 ('$ja4') happens to share Chrome 133's leading segment. Either OkHttp " +
                "shipped a fingerprint match (extremely unlikely) or this assertion needs a tighter " +
                "check."
        )
    }
}
