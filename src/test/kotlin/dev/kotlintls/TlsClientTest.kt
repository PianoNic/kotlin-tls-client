package dev.kotlintls

import dev.kotlintls.models.ClientIdentifier
import dev.kotlintls.models.DestroySessionPayload
import dev.kotlintls.models.GetCookiesFromSessionPayload
import dev.kotlintls.models.RequestMethod
import dev.kotlintls.models.RequestPayload
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import com.google.gson.JsonElement
import com.google.gson.JsonParser

class TlsClientTest {

    private val client = TlsClient()

    companion object {
        // httpbin.org is frequently rate-limited or down (503s), which made
        // these tests flaky in CI. Default to go-httpbin's public instance,
        // which is API-compatible and more reliable, and allow overriding the
        // host via the httpbin.baseUrl system property or HTTPBIN_BASE_URL env
        // var so CI can point at a self-hosted go-httpbin service container.
        private val BASE: String = (System.getProperty("httpbin.baseUrl")
            ?: System.getenv("HTTPBIN_BASE_URL")
            ?: "https://httpbingo.org").trimEnd('/')

        // go-httpbin returns header (and cookie) values as arrays of strings,
        // whereas the original httpbin returns plain strings. Accept both so
        // the assertions pass regardless of which instance BASE points at.
        private fun firstString(element: JsonElement): String =
            if (element.isJsonArray) element.asJsonArray[0].asString else element.asString
    }

    // ── Basic API ───────────────────────────────────────────────

    @Test
    fun `destroySession returns success`() {
        val result = client.destroySession(DestroySessionPayload("no-such-session"))
        assertTrue(result.success)
        assertNotNull(result.id)
    }

    @Test
    fun `getCookiesFromSession returns empty for unknown session`() {
        val result = client.getCookiesFromSession(GetCookiesFromSessionPayload("no-such-session", "https://example.com"))
        assertTrue(result.cookies.isEmpty())
        assertNotNull(result.id)
    }

    @Test
    fun `ClientIdentifier DEFAULT is Chrome 133`() {
        assertEquals(ClientIdentifier.CHROME_133, ClientIdentifier.DEFAULT)
        assertEquals("chrome_133", ClientIdentifier.DEFAULT.value)
    }

    @Test
    fun `fromString returns correct identifier`() {
        assertEquals(ClientIdentifier.CHROME_131, ClientIdentifier.fromString("chrome_131"))
        assertNull(ClientIdentifier.fromString("unknown"))
    }

    // ── HTTP requests ───────────────────────────────────────────

    @Test
    fun `GET httpbin returns 200`() {
        val resp = client.request(RequestPayload(requestUrl = "$BASE/get"))
        assertEquals(200, resp.status)
        assertTrue(resp.ok)
        assertTrue(resp.body.isNotBlank())
    }

    @Test
    fun `GET httpbin body is valid JSON with url field`() {
        val resp = client.request(RequestPayload(requestUrl = "$BASE/get"))
        assertEquals(200, resp.status)
        val json = JsonParser.parseString(resp.body).asJsonObject
        assertTrue(json.has("url"))
        assertEquals("$BASE/get", json.get("url").asString)
    }

    @Test
    fun `GET httpbin headers are sent correctly`() {
        val resp = client.request(
            RequestPayload(
                requestUrl = "$BASE/get",
                headers = mapOf("X-Test-Header" to "hello123")
            )
        )
        assertEquals(200, resp.status)
        val json = JsonParser.parseString(resp.body).asJsonObject
        val headers = json.getAsJsonObject("headers")
        assertEquals("hello123", firstString(headers.get("X-Test-Header")))
    }

    @Test
    fun `POST httpbin returns 200 with posted body`() {
        val resp = client.request(
            RequestPayload(
                requestUrl = "$BASE/post",
                requestMethod = RequestMethod.POST,
                requestBody = """{"key":"value"}""",
                headers = mapOf("Content-Type" to "application/json")
            )
        )
        assertEquals(200, resp.status)
        val json = JsonParser.parseString(resp.body).asJsonObject
        val data = json.get("data").asString
        assertTrue(data.contains("value"))
    }

    @Test
    fun `session persists cookies across requests`() {
        val sessionId = "test-session-cookies"
        // /cookies/set?name=value sets a cookie and redirects; use /cookies to
        // read it. Query form works on both httpbin and go-httpbin (the path
        // form /cookies/set/<name>/<value> 404s on go-httpbin).
        client.request(
            RequestPayload(
                requestUrl = "$BASE/cookies/set?testcookie=abc123",
                sessionId = sessionId,
                followRedirects = true
            )
        )
        val resp = client.request(
            RequestPayload(
                requestUrl = "$BASE/cookies",
                sessionId = sessionId
            )
        )
        assertEquals(200, resp.status)
        val json = JsonParser.parseString(resp.body).asJsonObject
        val cookies = json.getAsJsonObject("cookies")
        assertEquals("abc123", firstString(cookies.get("testcookie")))
        client.destroySession(DestroySessionPayload(sessionId))
    }

    @Test
    fun `redirect is followed when followRedirects is true`() {
        val resp = client.request(
            RequestPayload(
                requestUrl = "$BASE/redirect/1",
                followRedirects = true
            )
        )
        assertEquals(200, resp.status)
    }

    @Test
    fun `redirect is NOT followed when followRedirects is false`() {
        val resp = client.request(
            RequestPayload(
                requestUrl = "$BASE/redirect/1",
                followRedirects = false
            )
        )
        assertTrue(resp.status in 301..302)
    }

    @Test
    fun `response has usedProtocol set`() {
        val resp = client.request(RequestPayload(requestUrl = "$BASE/get"))
        assertTrue(resp.usedProtocol.isNotBlank())
    }

    @Test
    fun `destroyAll clears all sessions`() {
        val id1 = "session-a"
        val id2 = "session-b"
        client.request(RequestPayload(requestUrl = "$BASE/get", sessionId = id1))
        client.request(RequestPayload(requestUrl = "$BASE/get", sessionId = id2))
        val result = client.destroyAll()
        assertTrue(result.success)
    }

}
