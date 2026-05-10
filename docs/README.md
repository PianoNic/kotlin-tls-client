# kotlin-tls-client Documentation

## Getting started

- **[Getting Started](./getting-started.md)**: install, make your first request, pick a TLS profile
- [TLS Fingerprinting](./tls-fingerprinting.md): a short lesson on what JA3 is and why this library exists

## API reference

All public types live under `dev.kotlintls.*`. Only `TlsClient` is at the package root; everything else is in a subpackage.

- [TlsClient](./api/tls-client.md): low-level entrypoint (`request`, `destroySession`, `getCookiesFromSession`, `destroyAll`)
- [Session](./api/session.md): `get`, `post`, `put`, `delete`, `patch`, `head`, `options`, `close`, `cookies`
- [fetch](./api/fetch.md): one-shot request helper
- [Client](./api/client.md): process-wide singleton (`init`, `destroy`, `getInstance`)
- [Models](./api/models.md): `RequestPayload`, `ResponseData`, `RequestOptions`, `SessionOptions`, `Cookie`, `ClientIdentifier`, …
- [NativeTlsEngine](./api/native-engine.md): how the Go tls-client is loaded via JNA

## Internals

- [Architecture](./architecture.md): package layout, ports & adapters, how a request flows through the library
