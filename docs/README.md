# kotlin-tls-client Documentation

## Getting started

- **[Getting Started](./getting-started.md)** — Install, make your first request, pick a TLS profile
- [TLS Fingerprinting](./tls-fingerprinting.md) — A short lesson: what JA3 is and why this library exists

## API reference

All public types live under `dev.kotlintls.*`. Only `TlsClient` is at the package root; everything else is in a subpackage.

- [TlsClient](./api/tls-client.md) — Low-level entrypoint: `request`, `destroySession`, `getCookiesFromSession`, `destroyAll`
- [Session](./api/session.md) — `get`, `post`, `put`, `delete`, `patch`, `head`, `options`, `close`, `cookies`
- [fetch](./api/fetch.md) — One-shot request helper
- [Client](./api/client.md) — Process-wide singleton (`init`, `destroy`, `getInstance`)
- [Models](./api/models.md) — `RequestPayload`, `ResponseData`, `RequestOptions`, `SessionOptions`, `Cookie`, `ClientIdentifier`, …
- [NativeTlsEngine](./api/native-engine.md) — How the Go tls-client is loaded via JNA

## Internals

- [Architecture](./architecture.md) — Package layout, ports & adapters, how a request flows through the library
