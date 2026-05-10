package dev.kotlintls

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.kotlintls.client.fetch
import dev.kotlintls.models.ClientIdentifier
import dev.kotlintls.models.RequestMethod
import dev.kotlintls.models.SessionOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * "Did the disguise work?" tests.
 *
 * Hits https://tls.peet.ws/api/all with the library and asserts the resulting TLS / HTTP/2
 * fingerprint matches what a real browser would emit, not the JVM's TLS stack. If any of these
 * fail, the library is leaking enough of its true identity that a fingerprint-based bot detector
 * could flag it.
 *
 * Network-dependent; same trade-off as the existing httpbin-based tests.
 */
class TlsFingerprintTest {

    private val fpUrl = "https://tls.peet.ws/api/all"

    private fun probe(profile: ClientIdentifier): JsonObject {
        val resp = fetch(
            url = fpUrl,
            method = RequestMethod.GET,
            options = SessionOptions(clientIdentifier = profile, followRedirects = true)
        )
        assertEquals(200, resp.status, "tls.peet.ws returned non-200")
        return JsonParser.parseString(resp.body).asJsonObject
    }

    private fun JsonObject.tls(): JsonObject = getAsJsonObject("tls")
        ?: fail("Response missing 'tls' object")

    private fun JsonObject.http2OrNull(): JsonObject? =
        if (has("http2") && !get("http2").isJsonNull) getAsJsonObject("http2") else null

    private fun cipherIsGrease(name: String): Boolean {
        if (name.contains("GREASE", ignoreCase = true)) return true
        // GREASE values match 0x?A?A: 0x0A0A, 0x1A1A, 0x2A2A, ..., 0xFAFA
        val hex = Regex("(?i)0x([0-9a-f])a\\1a")
        return hex.containsMatchIn(name)
    }

    // ── Core "did it negotiate like a browser?" assertions ──────────────────

    @Test
    fun `chrome profile negotiates HTTP2 over TLS`() {
        val fp = probe(ClientIdentifier.CHROME_133)
        val httpVersion = fp.get("http_version").asString
        assertTrue(
            httpVersion.equals("h2", ignoreCase = true) || httpVersion.contains("HTTP/2"),
            "Chrome must negotiate HTTP/2 via ALPN; got '$httpVersion'. " +
                "If this is HTTP/1.1, ALPN didn't advertise h2 and the disguise is broken."
        )
        assertNotNull(fp.http2OrNull(), "Response is missing the http2 fingerprint section")
    }

    @Test
    fun `chrome cipher list includes GREASE values`() {
        val fp = probe(ClientIdentifier.CHROME_133)
        val ciphers = fp.tls().getAsJsonArray("ciphers").map { it.asString }
        val greasy = ciphers.filter(::cipherIsGrease)
        assertTrue(
            greasy.isNotEmpty(),
            "Chromium always injects GREASE into the cipher list. None found in $ciphers — " +
                "a fingerprint-based detector would flag this as not-actually-Chrome."
        )
    }

    @Test
    fun `chrome JA4 indicates TLS 1_3 and HTTP2`() {
        val fp = probe(ClientIdentifier.CHROME_133)
        val ja4 = fp.tls().get("ja4").asString
        assertTrue(
            ja4.startsWith("t13d"),
            "JA4 must start with t13d (TLS 1.3, domain protocol); got '$ja4'."
        )
        // JA4 format: t13d<XXYYzz>_..._...  where zz is the ALPN ('h2' or 'h1')
        // The leading segment ends with the ALPN, e.g. 't13d1516h2_...'.
        val firstSegment = ja4.substringBefore('_')
        assertTrue(
            firstSegment.endsWith("h2"),
            "JA4 first segment should end with 'h2' for HTTP/2; got '$firstSegment'."
        )
    }

    @Test
    fun `chrome HTTP2 SETTINGS frame uses chromium values`() {
        val fp = probe(ClientIdentifier.CHROME_133)
        val http2 = fp.http2OrNull() ?: fail("HTTP/2 layer missing")
        val frames = http2.getAsJsonArray("sent_frames").map { it.asJsonObject }
        val settingsFrame = frames.firstOrNull { it.get("frame_type")?.asString == "SETTINGS" }
            ?: fail("No SETTINGS frame seen in $frames")

        val settings = settingsFrame.getAsJsonArray("settings").map { it.asString }.joinToString(" | ")
        // Chromium signature: HEADER_TABLE_SIZE = 65536, INITIAL_WINDOW_SIZE = 6291456.
        // Other clients use very different values (Java's HttpClient uses 4096 / 16777216, etc.).
        assertTrue(
            settings.contains("HEADER_TABLE_SIZE") && settings.contains("65536"),
            "Chromium SETTINGS uses HEADER_TABLE_SIZE = 65536; got '$settings'."
        )
        assertTrue(
            settings.contains("INITIAL_WINDOW_SIZE") && settings.contains("6291456"),
            "Chromium SETTINGS uses INITIAL_WINDOW_SIZE = 6291456; got '$settings'."
        )
    }

    @Test
    fun `chrome pseudo-header order begins with method`() {
        val fp = probe(ClientIdentifier.CHROME_133)
        val http2 = fp.http2OrNull() ?: fail("HTTP/2 layer missing")
        val frames = http2.getAsJsonArray("sent_frames").map { it.asJsonObject }
        val headersFrame = frames.firstOrNull { it.get("frame_type")?.asString == "HEADERS" }
            ?: fail("No HEADERS frame seen")

        // peet.ws encodes the headers frame either as "headers" (raw list) or "pseudo_headers"
        // depending on version. Try both.
        val raw = headersFrame.get("pseudo_headers")?.asJsonArray
            ?: headersFrame.get("headers")?.asJsonArray
            ?: fail("HEADERS frame missing both pseudo_headers and headers fields")

        val first = raw.map { it.asString }.firstOrNull { it.startsWith(":") }
            ?: fail("No pseudo-header found in $raw")

        assertTrue(
            first.startsWith(":method"),
            "Chromium emits :method first in HEADERS; got '$first'. " +
                "Firefox starts with :method too, but Safari with :method as well — JVM HTTP/2 " +
                "stacks often differ. If this test starts failing, recheck the active profile."
        )
    }

    // ── Per-profile differentiation ──────────────────────────────────────────

    @Test
    fun `chrome and firefox profiles produce different JA4`() {
        val chrome = probe(ClientIdentifier.CHROME_133).tls().get("ja4").asString
        val firefox = probe(ClientIdentifier.FIREFOX_135).tls().get("ja4").asString
        assertNotEquals(
            chrome, firefox,
            "Chrome and Firefox must produce different JA4 fingerprints. " +
                "Same JA4 means both profiles emit the same handshake — the per-profile " +
                "differentiation isn't working."
        )
    }

    // ── Negative baseline: did we leak the JDK's own TLS fingerprint? ────────

    @Test
    fun `library JA4 differs from the JDK HttpClient JA4`() {
        val libraryJa4 = probe(ClientIdentifier.CHROME_133).tls().get("ja4").asString

        val jvm = HttpClient.newBuilder().build()
        val req = HttpRequest.newBuilder(URI.create(fpUrl)).GET().build()
        val jvmBody = jvm.send(req, HttpResponse.BodyHandlers.ofString()).body()
        val jvmJa4 = JsonParser.parseString(jvmBody).asJsonObject
            .getAsJsonObject("tls").get("ja4").asString

        assertNotEquals(
            libraryJa4, jvmJa4,
            "Library JA4 ($libraryJa4) matches the JDK HttpClient JA4 ($jvmJa4). " +
                "If they match, the library is leaking the JVM's TLS stack and a fingerprint-" +
                "based detector would flag every request as Java/JDK regardless of User-Agent."
        )
    }
}
