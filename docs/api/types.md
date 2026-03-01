# Types

All data classes and enums in the library.

## RequestPayload

Full request configuration. Used with `TlsClient.request()`.

```kotlin
RequestPayload(
    requestUrl = "https://example.com",        // required
    requestMethod = RequestMethod.GET,         // default: GET
    requestBody = null,                        // String?, body for POST/PUT/PATCH
    sessionId = null,                          // String?, omit for one-shot
    tlsClientIdentifier = "chrome_133",        // TLS profile (see ClientIdentifier)
    customTlsClient = null,                    // CustomTlsClient?, overrides identifier
    headers = mapOf("Accept" to "*/*"),
    headerOrder = listOf("accept", "user-agent"),
    followRedirects = false,
    insecureSkipVerify = false,
    timeoutSeconds = 30,
    timeoutMilliseconds = 0,                   // overrides timeoutSeconds if > 0
    proxyUrl = null,                           // "http://host:port"
    requestCookies = emptyList(),              // List<Cookie>
    withoutCookieJar = false,
    withRandomTLSExtensionOrder = false,
    forceHttp1 = false,
    disableHttp3 = false,
    disableIPV4 = false,
    disableIPV6 = false,
    transportOptions = null,
    certificatePinningHosts = emptyMap()
)
```

## ResponseData

Raw response returned by `TlsClient.request()`.

```kotlin
data.status          // Int: HTTP status code
data.body            // String: response body
data.headers         // Map<String, List<String>>
data.cookies         // Map<String, String>
data.target          // String: final URL
data.sessionId       // String?
data.usedProtocol    // "http/1.1" or "h2"
data.ok              // Boolean: status in 200..299
```

## Response

Returned by `Session` methods and `fetch`. Wraps `ResponseData` with a Node-compatible interface.

```kotlin
resp.ok              // Boolean
resp.status          // Int
resp.body            // String
resp.text()          // same as body
resp.headers         // Map<String, List<String>>
resp.cookies         // Map<String, String>
resp.url             // String: final URL
resp.usedProtocol    // String
resp.sessionId       // String?
```

## RequestMethod

```kotlin
enum class RequestMethod { GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS }
```

## ClientIdentifier

TLS profile to use. Sets the JA3 fingerprint (only with `NativeTlsEngine`; otherwise it's metadata only).

```kotlin
// Chrome
ClientIdentifier.CHROME_103 … CHROME_146, CHROME_133_PSK, …

// Firefox
ClientIdentifier.FIREFOX_102 … FIREFOX_147, FIREFOX_147_PSK

// Safari
ClientIdentifier.SAFARI_15_6_1, SAFARI_16_0
ClientIdentifier.SAFARI_IOS_15_5 … SAFARI_IOS_26_0

// Opera
ClientIdentifier.OPERA_89 … OPERA_91

// Mobile apps
ClientIdentifier.ZALANDO_ANDROID_MOBILE, NIKE_IOS_MOBILE, OKHTTP4_ANDROID_7 … OKHTTP4_ANDROID_13, …

// Default
ClientIdentifier.DEFAULT  // = CHROME_133
```

```kotlin
// Look up by string value
ClientIdentifier.fromString("chrome_133")   // ClientIdentifier.CHROME_133
ClientIdentifier.fromString("unknown")      // null
```

## Cookie

```kotlin
data class Cookie(
    val name: String,
    val value: String,
    val path: String = "/",
    val domain: String = "",
    val expires: Long = 0L,
    val maxAge: Int = -1,
    val secure: Boolean = false,
    val httpOnly: Boolean = false
)
```

## TransportOptions

```kotlin
data class TransportOptions(
    val disableKeepAlives: Boolean = false,
    val disableCompression: Boolean = false,
    val idleConnTimeout: Long? = null,
    val maxIdleConns: Int = 0,
    val maxIdleConnsPerHost: Int = 0,
    val maxConnsPerHost: Int = 0,
    val maxResponseHeaderBytes: Long = 0L,
    val writeBufferSize: Int = 0,
    val readBufferSize: Int = 0
)
```

## CustomTlsClient

Full custom TLS configuration. Use when you need a specific JA3 string.

```kotlin
data class CustomTlsClient(
    val ja3String: String,
    val h2Settings: Map<String, UInt> = emptyMap(),
    val h2SettingsOrder: List<String> = emptyList(),
    val pseudoHeaderOrder: List<String> = emptyList(),
    val certCompressionAlgos: List<String> = emptyList(),
    val supportedVersions: List<String> = emptyList(),
    val supportedSignatureAlgorithms: List<String> = emptyList(),
    val keyShareCurves: List<String> = emptyList(),
    val alpnProtocols: List<String> = emptyList(),
    val connectionFlow: UInt = 0u,
    // ...
)
```

## DestroySessionPayload / DestroySessionResponse

```kotlin
DestroySessionPayload(sessionId = "my-session")

data class DestroySessionResponse(val id: String, val success: Boolean)
```

## GetCookiesFromSessionPayload / GetCookiesFromSessionResponse

```kotlin
GetCookiesFromSessionPayload(sessionId = "my-session", url = "https://example.com")

data class GetCookiesFromSessionResponse(val id: String, val cookies: List<Cookie>)
```
