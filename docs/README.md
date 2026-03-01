# kotlin-tls-client Documentation

A Kotlin HTTP client with the same API as the Go and Node tls-clients.

## Quick Navigation

### Getting Started
- [Getting Started](./getting-started.md) – Installation and setup
- [Architecture](./architecture.md) – How the library is structured

### API Reference
- [TlsClient](./api/tls-client.md) – Low-level request, destroySession, getCookiesFromSession, destroyAll
- [Session](./api/session.md) – get, post, put, delete, patch, head, options, close, cookies
- [fetch](./api/fetch.md) – One-shot request
- [Client](./api/client.md) – Global singleton (init / destroy / getInstance)
- [Types](./api/types.md) – RequestPayload, ResponseData, SessionOptions, RequestOptions, Cookie, …
- [NativeTlsEngine](./api/native-engine.md) – Real browser fingerprint (JA3) via Go tls-client

### TLS Fingerprinting
- [TLS Fingerprinting](./TLS_FINGERPRINTING.md) – What it is and your options
- [What You Need To Do](./WHAT_YOU_NEED_TO_DO.md) – Step-by-step setup

### Building Native Libraries
- [Building Natives](./building-natives.md) – How to rebuild the `.so` files from source (WSL + Android NDK)

## What you can do

- **Session-based HTTP** – GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS with cookie jar and header order
- **One-shot fetch** – Single request without managing sessions
- **TLS profiles** – Set `clientIdentifier` to Chrome 103–146, Firefox, Safari, Opera, mobile apps
- **Custom TLS / JA3** – Pass a JA3 string or full `CustomTlsClient` config
- **Real browser fingerprint** – Use `NativeTlsEngine` with the Go tls-client native library
- **Proxy** – HTTP proxy per session or per request
- **Cookie jar** – Per-session cookies, `getCookiesFromSession`

## Getting help

- [Getting Started](./getting-started.md) for installation
- [Architecture](./architecture.md) for internals
