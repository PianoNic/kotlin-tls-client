# Architecture

How the kotlin-tls-client library is structured.

## Overview

```
Your application
       │
  ┌────┼──────────┐
  ▼    ▼          ▼
fetch  Session  TlsClient
         │          │
         └────┬─────┘
              ▼
         TlsClient
              │
       ┌──────┴──────┐
       ▼             ▼
   OkHttp       TlsClientEngine
  (default)     (NativeTlsEngine)
       │             │
       ▼             ▼
   HTTP/HTTPS    Go tls-client
                 (real JA3)
```

## Components

### `fetch`

Top-level function. Creates a temporary `Session`, performs one request, closes the session. No state is kept after the call.

### `Client`

Singleton that holds a shared `TlsClient`. Call `Client.init()` once at startup and `Client.destroy()` at shutdown. `fetch` uses it automatically if initialized; `Session` uses it when you pass `Client.getInstance()`.

### `Session`

Holds a `sessionId` and a `SessionOptions` config. Each method call (`get`, `post`, etc.) builds a `RequestPayload` and delegates to `TlsClient.request()`. Cookies are stored per-session by the underlying client. `close()` destroys the session.

### `TlsClient`

Core class. Two modes:
- **Default (OkHttp):** Uses OkHttp internally. Session state (cookies, connections) stored in a `ConcurrentHashMap` keyed by `sessionId`.
- **Native engine:** If constructed with `TlsClient(NativeTlsEngine())`, all calls are serialized to JSON and delegated to the Go tls-client via JNI. The Go library handles session state.

### `TlsClientEngine` / `NativeTlsEngine`

`TlsClientEngine` is an interface with four methods: `request`, `destroySession`, `getCookiesFromSession`, `destroyAll`. `NativeTlsEngine` implements it by calling the Go shared library through a C JNI bridge (`jni/tls_client_jni.c`).

### `Response` / `ResponseData`

`ResponseData` is the raw data class (status, body, headers, cookies, target, sessionId, usedProtocol). `Response` wraps it with a Node-compatible interface (`ok`, `text()`, `url`, etc.).

## Data flow

### OkHttp path

```
Session.get(url)
  → TlsClient.request(RequestPayload)
    → getOrCreateSession(sessionId)   // creates OkHttpClient + MutableCookieJar
    → OkHttpClient.newCall(request).execute()
    → ResponseData(status, body, headers, cookies, ...)
  → Response(data)
```

### Native engine path

```
Session.get(url)
  → TlsClient.request(RequestPayload)
    → payload.toRequestJson()          // serialize to JSON string
    → NativeTlsEngine.request(json)
      → JNI → tls_client_jni.c
        → dlsym("request") → Go tls-client
        → returns JSON string
    → json.parseResponseJson()         // deserialize
  → Response(data)
```

## Class hierarchy

```
Client (singleton)
  └── TlsClient
        ├── MutableCookieJar (per session, OkHttp path)
        └── TlsClientEngine
              └── NativeTlsEngine

Session (wraps TlsClient + sessionId)
  └── Response (wraps ResponseData)

fetch (top-level function, creates a temporary Session)
```

## Thread safety

`TlsClient` uses a `ConcurrentHashMap` for session storage and a `synchronized` block for session creation. It is safe to use from multiple threads. `Client` uses double-checked locking. `Session` is not thread-safe — use one session per thread or synchronize externally.
