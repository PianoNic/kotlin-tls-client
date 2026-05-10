package dev.kotlintls

import dev.kotlintls.engine.NativeTlsEngine
import dev.kotlintls.engine.TlsClientEngine
import dev.kotlintls.internal.parseDestroySessionResponse
import dev.kotlintls.internal.parseGetCookiesResponse
import dev.kotlintls.internal.parseResponseJson
import dev.kotlintls.internal.toJson
import dev.kotlintls.internal.toRequestJson

class TlsClient @JvmOverloads constructor(
    private val engine: TlsClientEngine = NativeTlsEngine()
) {

    fun request(payload: RequestPayload): ResponseData {
        val json = payload.toRequestJson()
        val responseJson = engine.request(json)
        return responseJson.parseResponseJson()
    }

    fun destroySession(payload: DestroySessionPayload): DestroySessionResponse {
        val out = engine.destroySession(payload.toJson())
        return out.parseDestroySessionResponse()
    }

    fun getCookiesFromSession(payload: GetCookiesFromSessionPayload): GetCookiesFromSessionResponse {
        val out = engine.getCookiesFromSession(payload.toJson())
        return out.parseGetCookiesResponse()
    }

    fun destroyAll(): DestroySessionResponse {
        val out = engine.destroyAll()
        return out.parseDestroySessionResponse()
    }
}
