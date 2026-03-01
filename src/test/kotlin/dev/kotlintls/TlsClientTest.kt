package dev.kotlintls

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import com.google.gson.JsonParser

class TlsClientTest {

    private val client = TlsClient()

    // ── Basic API ────────────────────────────────────────────────────────────

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

    // ── HTTP requests ────────────────────────────────────────────────────────

    @Test
    fun `GET httpbin returns 200`() {
        val resp = client.request(RequestPayload(requestUrl = "https://httpbin.org/get"))
        assertEquals(200, resp.status)
        assertTrue(resp.ok)
        assertTrue(resp.body.isNotBlank())
    }

    @Test
    fun `GET httpbin body is valid JSON with url field`() {
        val resp = client.request(RequestPayload(requestUrl = "https://httpbin.org/get"))
        assertEquals(200, resp.status)
        val json = JsonParser.parseString(resp.body).asJsonObject
        assertTrue(json.has("url"))
        assertEquals("https://httpbin.org/get", json.get("url").asString)
    }

    @Test
    fun `GET httpbin headers are sent correctly`() {
        val resp = client.request(
            RequestPayload(
                requestUrl = "https://httpbin.org/get",
                headers = mapOf("X-Test-Header" to "hello123")
            )
        )
        assertEquals(200, resp.status)
        val json = JsonParser.parseString(resp.body).asJsonObject
        val headers = json.getAsJsonObject("headers")
        assertEquals("hello123", headers.get("X-Test-Header").asString)
    }

    @Test
    fun `POST httpbin returns 200 with posted body`() {
        val resp = client.request(
            RequestPayload(
                requestUrl = "https://httpbin.org/post",
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
        // httpbin /cookies/set sets a cookie and redirects; use /cookies to read them
        client.request(
            RequestPayload(
                requestUrl = "https://httpbin.org/cookies/set/testcookie/abc123",
                sessionId = sessionId,
                followRedirects = true
            )
        )
        val resp = client.request(
            RequestPayload(
                requestUrl = "https://httpbin.org/cookies",
                sessionId = sessionId
            )
        )
        assertEquals(200, resp.status)
        val json = JsonParser.parseString(resp.body).asJsonObject
        val cookies = json.getAsJsonObject("cookies")
        assertEquals("abc123", cookies.get("testcookie").asString)
        client.destroySession(DestroySessionPayload(sessionId))
    }

    @Test
    fun `redirect is followed when followRedirects is true`() {
        val resp = client.request(
            RequestPayload(
                requestUrl = "https://httpbin.org/redirect/1",
                followRedirects = true
            )
        )
        assertEquals(200, resp.status)
    }

    @Test
    fun `redirect is NOT followed when followRedirects is false`() {
        val resp = client.request(
            RequestPayload(
                requestUrl = "https://httpbin.org/redirect/1",
                followRedirects = false
            )
        )
        assertTrue(resp.status in 301..302)
    }

    @Test
    fun `response has usedProtocol set`() {
        val resp = client.request(RequestPayload(requestUrl = "https://httpbin.org/get"))
        assertTrue(resp.usedProtocol.isNotBlank())
    }

    @Test
    fun `destroyAll clears all sessions`() {
        val id1 = "session-a"
        val id2 = "session-b"
        client.request(RequestPayload(requestUrl = "https://httpbin.org/get", sessionId = id1))
        client.request(RequestPayload(requestUrl = "https://httpbin.org/get", sessionId = id2))
        val result = client.destroyAll()
        assertTrue(result.success)
    }
}
