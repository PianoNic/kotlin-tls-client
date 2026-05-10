package dev.kotlintls.internal

internal data class WsOpenPayload(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val headerOrder: List<String> = emptyList(),
    val tlsClientIdentifier: String = "chrome_133",
    val withRandomTLSExtensionOrder: Boolean = true,
    val handshakeTimeoutMilliseconds: Int = 30_000,
    val insecureSkipVerify: Boolean = false,
    val proxyUrl: String? = null,
)

internal data class WsOpenResponse(
    val ok: Boolean = false,
    val connId: String? = null,
    val error: String? = null,
)

internal data class WsResultResponse(
    val ok: Boolean = false,
    val error: String? = null,
)

/**
 * Discriminated union returned by wsRecv. `type` is one of:
 *   "text"    | data
 *   "binary"  | data (base64)
 *   "close"   | code, reason
 *   "error"   | error
 *   "timeout" | (no fields)
 *   "unknown" | code (raw frame opcode)
 */
internal data class WsFrame(
    val type: String? = null,
    val data: String? = null,
    val code: Int? = null,
    val reason: String? = null,
    val error: String? = null,
)
