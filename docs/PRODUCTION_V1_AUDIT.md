# GitHub Rock v1.0 production audit

Audit baseline: `main` at commit `76d11724967acf307c12398a0a301c01e2a61a37` (2026-08-10).

This is an evidence-based starting point, not a claim that v1.0 is complete.

## Current baseline

- Native Android app in one `app` module, Kotlin, Jetpack Compose, Material 3, Hilt, Retrofit/OkHttp, Room, DataStore, WorkManager, Coil, and Navigation Compose.
- Android API range is 29 through 36. Release minification and optional environment-backed signing are configured.
- Existing product surface includes authentication, guest/demo modes, Home, repositories, repository details, issues, pull requests, Actions/build monitoring, downloads, APK inspection, profile, settings, deep links, and adaptive navigation.
- The codebase already has a repository/data/UI separation and a documented alpha status. The safest path is hardening and consolidation, not a rewrite.
- Recent history shows active incremental work on loading states, transitions, accessibility, markdown rendering, APK inspection tests, and release tooling.

## Immediate risks and gaps

### Release and CI

- The repository has strong CI intent, but workflows generate the Gradle wrapper instead of verifying a committed wrapper. This weakens reproducibility and should be corrected before v1.0.
- CI runs unit tests, debug UI-test compilation, lint, debug build, and release build, but it does not yet demonstrate connected Compose execution, migration tests, security scanning, dependency updates, or a reproducible signed AAB path.
- `versionName` defaults to `0.1.0`; v1.0 must move versioning to an explicit release-only configuration and publish both APK and AAB where applicable.
- Action references are floating major tags. Critical actions should be reviewed and pinned according to the repository's maintenance policy.

### Authentication and security

- Device Flow, Keystore-backed storage, redacted logging, cleartext blocking, and release certificate verification are present and are good foundations.
- The OAuth client ID is intentionally public, but the authentication setup and callback documentation must be reconciled with the actual Device Flow implementation. No client secret may enter the APK or CI logs.
- The manifest requests broad install/delete package, foreground-service, boot, and Termux permissions. Each permission needs a feature-level justification, runtime gating, and a release review. Remove anything not required by the shipped Android experience.
- The HTTPS GitHub deep-link filter is broad. Incoming URLs must be parsed into a strict allow-list before any native navigation, with all other GitHub URLs handed to the browser.
- Biometric app lock, token cleanup verification, and session-recovery tests remain release-blocking gaps unless already covered elsewhere in the source tree.

### Architecture and data

- The architecture is serviceable, but the next pass should enforce immutable screen state, cached-first repository reads, typed domain errors, request deduplication, rate-limit handling, and consistent retry/backoff at repository boundaries.
- Room and WorkManager are present. Verify indexes, migrations, unique download identity, process-death recovery, network constraints, and notification permission behaviour with tests rather than relying on happy-path UI.
- Paging dependencies are present, while the documented roadmap still calls out Paging-backed large lists. Confirm actual adoption screen by screen and remove unused dependencies or complete the migration.

### UI, accessibility, and large screens

- Adaptive navigation and appearance controls already exist. Finish centralizing spacing, shapes, typography, colors, and semantic status components so screens cannot drift into one-off values.
- Every major destination needs verified loading, success, empty, error, offline, and refresh states. Existing documentation says this is mostly present, but physical-device and screenshot validation are still required.
- TalkBack, large text, keyboard/focus navigation, contrast, reduced motion, and foldable list-detail layouts are not yet evidenced as complete.
- README screenshots are explicitly still missing. v1.0 cannot be marketed as polished until real device captures are added.

### Documentation

The requested v1 documentation set is incomplete. Add and keep current: `BUILD.md`, `ARCHITECTURE.md`, `UI_UX_DESIGN.md`, `CONTRIBUTING.md` at the documented root location if desired, `CODE_OF_CONDUCT.md`, `CHANGELOG.md`, and `PRIVACY.md`. Existing `.github` policy files and root privacy/security documents should be linked consistently rather than duplicated with conflicting instructions.

## Delivery order

1. Build reproducibility: commit and verify the Gradle wrapper, normalize CI tasks, and add dependency/security checks.
2. Security hardening: audit manifest permissions, deep links, token lifecycle, logging, external intents, file providers, and APK parsing.
3. Data reliability: typed errors, cache freshness, pagination, rate limits, retry policy, download identity, and migration/process-death tests.
4. UI system: central tokens, reusable state components, accessibility semantics, reduced motion, and adaptive list-detail layouts.
5. Core journeys: login, attention-first Home, repository search/detail, PR/issue review, Actions/build monitoring, artifact download, and APK inspection.
6. Release readiness: real screenshots, version 1.0.0, signed APK/AAB, checksums, changelog, privacy/security review, and physical-device validation.

## Release gate

Do not label the app v1.0-ready until debug and release builds, lint, unit tests, Compose tests, migration tests, security checks, signing verification, and the critical login-to-artifact journey all pass in CI. Current evidence supports a strong alpha foundation, not a 95–100 score.

## First implementation slice

The first code slice after this audit should be small and independently verifiable: make CI use the committed wrapper, add explicit `check` coverage for lint/tests/UI-test compilation, and add a security-focused manifest/deep-link test. Larger UI and feature work should follow only after that baseline is green.
