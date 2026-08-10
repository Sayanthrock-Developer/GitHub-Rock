# GitHub Rock architecture

GitHub Rock is a native Android application. The Android client owns the user interface, GitHub-connected workflows, local cache, downloads, and APK inspection. Desktop and backend-dependent capabilities must remain explicitly separated from the mobile app.

## Runtime layers

```text
Compose UI
  -> ViewModel and immutable UI state
  -> domain/repository operations
  -> GitHub REST/GraphQL, Room cache, DataStore, WorkManager
```

### UI

`ui/` contains navigation, screens, reusable components, themes, loading/error/empty states, and adaptive layouts. Screens should collect lifecycle-aware state and send user intents to ViewModels. They must not make network calls directly.

### State and domain coordination

ViewModels expose immutable state and coordinate refresh, pagination, authentication state, and user actions. Long-running or process-resilient work belongs in WorkManager, not in a composable or activity callback.

### Data

`data/` contains authentication, GitHub API access, repositories, local Room persistence, DataStore settings, demo data, and backend contracts. Repository methods own caching, freshness, error classification, rate-limit handling, and retry policy. Tokens and authorization material never cross into UI state or logs.

### Downloads and build monitoring

`download/` and `build/` contain resumable download and workflow-monitoring policies, workers, notifications, and typed progress models. Download identity must be stable across process death, and APK parsing must operate on downloaded files without executing them.

## Security boundaries

- GitHub Device Flow collects no password and stores only protected session material.
- Android Keystore protects local credentials. Release logs must redact tokens, authorization codes, secrets, and sensitive personal data.
- External URLs pass through an allow-list. Repository deep links may enter native navigation; unrelated GitHub URLs open in the browser.
- APKs are treated as untrusted input and are parsed in an isolated, non-executing path.
- Backend and companion features may expose status and typed operations to Android, but Android must not contain shared private keys, unrestricted shell access, database credentials, or long-lived provider secrets.

## Change rules

1. Inspect the existing path before changing it.
2. Keep the smallest safe change and preserve working behaviour.
3. Add or update unit and UI tests with user-visible behaviour changes.
4. Verify loading, success, empty, error, offline, accessibility, and process-death states where relevant.
5. Run lint, unit tests, debug build, release build, and relevant instrumentation checks before calling a slice complete.
