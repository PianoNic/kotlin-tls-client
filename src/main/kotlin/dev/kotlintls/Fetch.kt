package dev.kotlintls

/**
 * One-shot fetch: creates a session, performs one request, then closes the session.
 * Matches Node fetch(url, options) API.
 * Uses Client.getInstance() if Client.init() was called; otherwise creates a temporary TlsClient.
 */
fun fetch(
    url: String,
    method: RequestMethod = RequestMethod.GET,
    options: SessionOptions = SessionOptions(),
    requestOptions: RequestOptions = RequestOptions()
): Response {
    val client = if (Client.isReady()) Client.getInstance() else TlsClient()
    val session = Session(client, options)
    return try {
        when (method) {
            RequestMethod.GET -> session.get(url, requestOptions)
            RequestMethod.POST -> session.post(url, requestOptions)
            RequestMethod.PUT -> session.put(url, requestOptions)
            RequestMethod.DELETE -> session.delete(url, requestOptions)
            RequestMethod.PATCH -> session.patch(url, requestOptions)
            RequestMethod.HEAD -> session.head(url, requestOptions)
            RequestMethod.OPTIONS -> session.options(url, requestOptions)
        }
    } finally {
        session.close()
    }
}
