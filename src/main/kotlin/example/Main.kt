import dev.kotlintls.*

/**
 * Example matching Go tls-client and Node node-tls-client usage.
 */
fun main() {
    // 1) One-shot fetch (no init)
    val response = fetch("https://httpbin.org/get", RequestMethod.GET)
    println("fetch status: ${response.status}, ok: ${response.ok}")
    println("body: ${response.body.take(200)}...")

    // 2) Session-based (like Node Session)
    Client.init()
    val session = Session(Client.getInstance(), SessionOptions(
        clientIdentifier = ClientIdentifier.CHROME_131,
        followRedirects = false,
        timeout = 30_000
    ))
    val getResp = session.get("https://httpbin.org/get", RequestOptions(
        headers = mapOf(
            "Accept" to "*/*",
            "User-Agent" to "Mozilla/5.0 (compatible; kotlin-tls-client/1.0)"
        )
    ))
    println("session get status: ${getResp.status}")
    session.close()

    // 3) Low-level request payload (like Go/Node FFI)
    val client = TlsClient()
    val data = client.request(RequestPayload(
        requestUrl = "https://httpbin.org/post",
        requestMethod = RequestMethod.POST,
        requestBody = """{"hello":"world"}""",
        tlsClientIdentifier = ClientIdentifier.CHROME_133.value,
        sessionId = null,
        followRedirects = false,
        timeoutSeconds = 30,
        headers = mapOf("Content-Type" to "application/json")
    ))
    println("request status: ${data.status}, target: ${data.target}")
    client.destroyAll()
    Client.destroy()
}
