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

This library wraps [bogdanfinn/tls-client](https://github.com/bogdanfinn/tls-client) (Go) via JNA to provide TLS fingerprint impersonation from Kotlin/JVM and Android.

### Key layers

- **`TlsClient`** — public entry point. All requests go through the native Go TLS engine. Defaults to `NativeTlsEngine()`.
- **`NativeTlsEngine`** — calls Go `tls-client` shared library via JNA. Handles JSON serialization of requests/responses.
- **`GoTlsClient`** — JNA interface that maps Go shared library functions to Kotlin.
- **`NativeLibLoader`** — extracts and loads bundled native libraries per platform via JNA. Falls back to `System.loadLibrary()` on unsupported platforms or Android.
- **`ClientIdentifier`** — enum of browser TLS profiles (Chrome, Firefox, Safari, Opera, etc.). Default: `CHROME_133`.

### Native libraries

- **Not stored in git** — downloaded at build time by Gradle `downloadNatives` task
- Built via fork [`PianoNic/kotlin-tls-client-natives`](https://github.com/PianoNic/kotlin-tls-client-natives) (auto-syncs with upstream bogdanfinn/tls-client)
- Version pinned in `natives-version.txt`, bumped automatically via PR when fork releases a new version
- Cached in `build/natives/` — only downloads once until `./gradlew clean`

### Build

- Gradle (Kotlin DSL). JitPack-compatible.
- JAR version derived from git tags, not hardcoded.
- Android: uses `System.loadLibrary` (W^X policy — no temp dir extraction).
