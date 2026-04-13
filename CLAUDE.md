# kotlin-tls-client — working conventions

These are the conventions to follow when contributing to this repo (humans or AI assistants).

## Issues

- Title: short, imperative, sentence case. Examples: `Add proxy rotation support`, `Fix NPE when Gson returns null for non-JSON responses`.
- Body: 1–2 sentences explaining **what** and **why**. No checklists, no markdown headers, no "acceptance criteria" sections.
- One issue = one focused change.
- **Every issue MUST have at least one label.** Pick from the repo's existing labels (`bug`, `enhancement`, `feature`, `refactor`, `documentation`, `duplicate`, `stale`). Never open an unlabeled issue.

## Branches

- Pattern: `feature/<issueNumber>_<PascalCaseName>`
- Example: `feature/12_ProxyRotationSupport`
- One branch per issue. No multi-issue branches.

## Commits

- Short, descriptive, imperative. No prefixes like `feat:`, `fix:`, `chore:`, etc.
- Examples: `Make ResponseJson fields nullable for Gson safety`, `Use System.loadLibrary on Android instead of extracting to tmpdir`.
- **No Claude / AI attribution.** No `Co-Authored-By: Claude ...`, no `Generated with ...` trailers, no mention of AI assistance anywhere in the commit message.

## Pull requests

- Title mirrors the commit / issue title.
- Body: 1–2 sentences + `Closes #<issue>`. No AI attribution.
- **Every PR MUST have at least one label.** Same label rules as issues. Verify with `gh pr view <n> --json labels` after creating.

## Architecture

This library wraps [bogdanfinn/tls-client](https://github.com/bogdanfinn/tls-client) (Go) via JNI to provide TLS fingerprint impersonation from Kotlin/JVM and Android.

### Key layers

- **`TlsClient`** — public entry point. If a `NativeTlsEngine` is provided, requests go through the Go TLS library with browser fingerprinting. Without an engine, requests fall back to plain OkHttp (no impersonation).
- **`NativeTlsEngine`** — JNI bridge to the Go `tls-client` shared library. Handles JSON serialization of requests/responses.
- **`NativeLibLoader`** — extracts and loads bundled native libraries per platform. Falls back to `System.loadLibrary()` on unsupported platforms or Android.
- **`ClientIdentifier`** — enum of browser TLS profiles (Chrome, Firefox, Safari, Opera, etc.). Default: `CHROME_133`.

### OkHttp fallback

When no `NativeTlsEngine` is provided, `TlsClient` uses plain OkHttp. This is intentional — not every use case needs fingerprint impersonation, and not every platform has native libs available. Do not remove or warn about this fallback.

### Native libraries

- Bundled under `src/main/resources/dev/kotlintls/natives/{platform}/`
- Updated automatically via GitHub Actions (daily check against bogdanfinn releases)
- Version tracked in `natives-version.txt`
- JNI bridges built in CI for each platform

### Build

- Gradle (Kotlin DSL). JitPack-compatible.
- JAR version derived from git tags, not hardcoded.
- Android: uses `System.loadLibrary` (W^X policy — no temp dir extraction).
