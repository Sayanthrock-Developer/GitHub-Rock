# GitHub Rock

GitHub Rock is a native Android developer control centre for GitHub. It combines secure GitHub authentication, repository browsing, issues and pull-request visibility, Actions monitoring, Android build workflow generation, managed artifact downloads, and APK inspection in one Kotlin/Compose application.

> Status: **first functional alpha**. The implemented screens and API calls are real. Demo mode is clearly labelled and isolated. Features listed under “Planned next” are intentionally not presented as complete in the app.

## Highlights

- Kotlin, Jetpack Compose, Material 3, Gradle Kotlin DSL, and a version catalog
- One stable `app` module, organized by feature and layer
- MVVM with a pragmatic Clean Architecture boundary
- Hilt, Retrofit/OkHttp, Kotlin Serialization, Room, DataStore, Paging dependencies, WorkManager, Coil, and Navigation Compose
- Android 10+ (`minSdk 29`), `compileSdk` / `targetSdk` 36
- GitHub OAuth App Device Flow with explicit scopes, pending, slow-down, expired, denied, refresh handling, and an official GitHub account-signup link
- Searchable GitHub services hub with 45 allow-listed official destinations for notifications, account queues, Codespaces, Projects, Gists, Marketplace, Copilot, enterprise, accessibility, security, billing, settings, and community features
- Guest access for public repositories and a fully isolated demo workspace
- Own-profile mobile view with follower statistics, pronouns, yearly contributions, highlights, organizations, public links, ORCID detection, and an official achievements destination
- Repository discovery with language/type/sort controls and a direct New repository action
- Full workflow logs in a configurable scrollable popup or high-performance, syntax-highlighted terminal with line numbers and copy-all
- In-app application, Android SDK, device, installation, and permission information
- Connected profile with public repository count, API rate-limit health, repository search/cache foundation, workflow runs, issues, pull requests, code directory listings, and releases
- Platform-aware GitHub Release picker for Android, Windows, Linux, iOS, and macOS assets, with file-format and architecture guidance
- Deterministic Android project detection and safe workflow generation for `assembleDebug` and `assembleRelease`, followed by reviewed-branch PR creation, merged-workflow dispatch, durable run tracking, completion notifications, and artifact handoff to Downloads
- Background download queue with live byte progress, pause/resume, confirmed cancel, retry without duplicate history, SHA-256 fingerprinting and expected-checksum verification, duplicate-safe file finalization, and Room recovery
- APK metadata, permission, SDK, signing fingerprint, installed-signature comparison, and file hash inspection foundation
- Clean adaptive Material 3 visual system with consistent spacing, typography, grouped settings, edge-to-edge system bars, phone bottom navigation, tablet/landscape navigation rail, system/light/dark modes, true black, five accent choices, and optional dynamic color
- Pull-to-refresh dashboard with honest initial-loading, refreshing, empty, and offline feedback; account, rate-limit, and repository requests are loaded concurrently, and repository artwork is batched and cached for the app session
- Direct API-backed deep links for public, private, and unlisted repositories, plus builds, releases, and standard GitHub repository URLs
- Unit tests for authentication responses, workflow status, Android workflow generation, project detection, release-asset classification, dispatched-run matching, completion notification policy, safe refs, and checksums
- Compose UI test for login and entry navigation

## Screenshots

| Screen | What it shows |
| --- | --- |
| Login | Explicit sign-in, official signup (Google, Apple, or email), GitHub Device Flow, guest access, and demo mode |
| Home | Clear account/API status, repository and build metrics, quick actions, pull-to-refresh, and honest loading/empty states |
| Repositories | Public/authorized repository search and repository cards |
| Repository | Overview, Code, Issues, Pull Requests, Actions, and a five-platform Release asset picker |
| Builds | APK workflow preview/PR creation, merged-workflow detection, dispatch, live job state, background completion monitoring, and artifact handoff |
| Downloads | Explicit GitHub image/file downloads, byte-based transfer details, pause/resume/cancel/restart controls, SHA-256 fingerprints, sharing, and APK inspection |
| Profile | Own-account summary with repository/follower/following links, contributions, highlights, GitHub security, appearance, feature status, and session controls |
| Appearance | Theme, typography, log style, bulk optional-feature controls (off by default), true black, dynamic color, and persistent accents |
| All GitHub | Native feature status plus secure access to every major GitHub.com workspace and account tool |

PNG screenshots will be added after the first instrumented device capture. No mock screenshot is presented as a running build.

## Architecture

```text
Compose UI + Navigation
        ↓
ViewModels / immutable UI state
        ↓
Repositories (auth, GitHub, settings, downloads)
        ↓
Retrofit APIs • Room • DataStore • Keystore token store • WorkManager
```

The project deliberately starts as one application module. Packages preserve clear boundaries without the build complexity of premature multi-module architecture.

```text
app/src/main/java/com/sayanthrock/githubrock/
├── core/       models, networking contracts, security, checksum/APK/build utilities
├── data/       authentication, GitHub repository, Room, DataStore, demo data
├── di/         Hilt bindings and providers
├── download/   recoverable background download worker
└── ui/         Compose theme, components, navigation, screens, view models
```

## OAuth App registration and Device Flow

> Full setup guide: [GitHub authentication and APK signing](docs/GITHUB_AUTH_AND_SIGNING.md).

1. Open **GitHub Settings → Developer settings → OAuth Apps → New OAuth App**.
2. Use application name `Sayanth Rock Mobile Oauth`.
3. Use homepage URL `https://github.com/Sayanthrock-Developer/GitHub-Rock`.
4. Use callback URL `githubrock://oauth/callback`. GitHub requires this registration value, but the current Device Flow login does not use a callback token exchange.
5. Enable **Device Flow** and save the OAuth App.
6. Use the public Client ID `Ov23lim8WhLjeUMqvuMj`. Never place the Client Secret, access token, keystore, private key, or password inside the Android project.
7. Copy `local.properties.example` to `local.properties` for local development.

```properties
sdk.dir=/absolute/path/to/Android/Sdk
GITHUB_CLIENT_ID=Ov23lim8WhLjeUMqvuMj
```

The Client ID is exposed through `BuildConfig.GITHUB_CLIENT_ID` and is bundled as the official fallback so CI and release builds can sign in. Forks can override it with a `PUBLIC_GITHUB_OAUTH_CLIENT_ID` repository variable under **Settings → Secrets and variables → Actions → Variables**. A Client ID is public; client secrets and tokens must never be bundled.

New users can tap **Sign up for GitHub** to open GitHub's official signup page in an Ephemeral Custom Tab. The app then opens GitHub's official Device Flow authorization page, displays a copyable verification code, and polls only at GitHub's supplied interval. GitHub Rock never asks for a GitHub password.

## OAuth scopes

GitHub Rock requests the following scopes during Device Flow authorization:

| Scope | Used for |
| --- | --- |
| `repo` | Public/private repositories, issues, pull requests, releases, and Actions data |
| `workflow` | Creating or updating GitHub Actions workflow files |
| `read:user` | Connected account profile |
| `user:email` | Account email addresses when required |
| `read:org` | Organization membership and reviewer/assignee resolution |
| `notifications` | GitHub notifications used by the app |

Users approve these permissions on GitHub's official page. Removing a scope requires signing out and authorizing again with the new scope set.

## Build

Use Android Studio with JDK 17 and Android SDK 36. The CI workflow bootstraps Gradle 8.13 and generates the wrapper before verification. To bootstrap locally once:

```bash
gradle wrapper --gradle-version 8.13
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

The debug APK is produced under `app/build/outputs/apk/debug/`.

## Android build workflow setup

`.github/workflows/android-build.yml` provides a manually dispatched build for:

- Debug APK → `:app:assembleDebug`
- Release APK → `:app:assembleRelease`

GitHub Rock's `AndroidProjectDetector` looks for `gradlew`, Gradle settings/build files, Android manifests, application-module paths, and existing workflow files. `AndroidWorkflowGenerator` maps a validated module name to a fixed APK Gradle task; it rejects shell syntax instead of interpolating arbitrary commands.

When adding a workflow to another repository, GitHub Rock previews the complete YAML, creates a new branch, commits the file, and opens a pull request. After that workflow is reviewed and merged, the Builds tab detects it on the repository, dispatches a selected branch or tag, identifies the new run, and follows job/step state to completion. A network-constrained WorkManager job persists run discovery across process loss and posts an honest terminal-state notification when Android notification permission is available. Published artifacts can then be queued in Downloads. Branch protection is never bypassed.

## Release signing

Create these GitHub Actions repository or environment secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Independently read the SHA-256 certificate fingerprint from the intended release keystore and create the public Actions variable `EXPECTED_RELEASE_CERT_SHA256`. The release workflow fails closed if this value is missing, malformed, or does not match the APK signer.

The workflow decodes the keystore into the runner's temporary directory and exposes passwords only as masked environment variables. `app/build.gradle.kts` activates the release signing configuration only when `GITHUB_ROCK_KEYSTORE_PATH` exists in the build environment. Secret values are never written into YAML, source, logs, artifacts, or the APK.

Each published release includes both the APK file checksum (`.apk.sha256`) and the signing-certificate fingerprint (`.apk.certificate.sha256`). The certificate asset is a convenience copy, not the root of trust; compare it with the independently published trusted project fingerprint.

## Publish and install the app

1. Add the four Android signing secrets listed above and set `EXPECTED_RELEASE_CERT_SHA256` from the independently verified release keystore. The official public OAuth Client ID is already configured; forks may override it with `PUBLIC_GITHUB_OAUTH_CLIENT_ID`.
2. Open **Actions → Publish Android Release → Run workflow**.
3. Enter a new version such as `0.2.0` and choose whether it is a prerelease.
4. Wait for pinned certificate verification, checksum generation, and release publication to finish.
5. Open the repository's **Releases** page and download `GitHub-Rock-<version>.apk`, `GitHub-Rock-<version>.apk.sha256`, and `GitHub-Rock-<version>.apk.certificate.sha256`.
6. Compare the APK's SHA-256 checksum with the `.apk.sha256` file. Compare the detected signing certificate with the fingerprint previously published through an independently trusted project channel.
7. On Android 10 or newer, allow **Install unknown apps** only for the browser or file manager you used, open the APK, and approve Android's official Package Installer prompt.

If Android reports an incompatible signature, the installed copy was signed with a different key. Uninstall that older release before installing the new APK, or rebuild with the original signing key. Uninstalling removes that app's local data. Never bypass Play Protect or Android's package-signature checks.

## Security

- The APK contains only the public OAuth Client ID; no OAuth Client Secret is embedded or required.
- OAuth Device Flow requests explicit scopes and uses GitHub's official authorization page.
- Access and refresh tokens are kept in `EncryptedSharedPreferences` using an Android Keystore AES-256-GCM master key and are excluded from backups.
- `Authorization` and `Cookie` headers are redacted; release builds disable HTTP logging.
- The central GitHub REST version header is `BuildConfig.GITHUB_API_VERSION`.
- The network stack rejects cleartext traffic.
- APK installation is delegated to Android's official package installer; there is no silent install or security bypass.
- Release publication verifies the APK signer against `EXPECTED_RELEASE_CERT_SHA256` before creating release assets.
- SHA-256 is calculated locally for every completed file. Publisher identity is established only when that fingerprint is compared with a trusted checksum or signature.
- Destructive GitHub operations require an explicit confirmation in the product flow.
- Website-only actions open through an allow-listed HTTPS GitHub host in a Custom Tab. Credentials, passkeys, tokens, billing details, and Marketplace purchases remain on GitHub's pages and are never collected by GitHub Rock.
- Demo records use negative IDs, are loaded from an isolated provider, and are never merged with connected account data.
- Build notification permission is requested only when the user starts a monitored build; denying it does not block the workflow.

See [SECURITY.md](SECURITY.md) for reporting guidance.

## Current functional scope

- OAuth Device Flow session acquisition/storage/refresh/logout foundation
- Guest and isolated demo entry
- Connected dashboard request with API status, active/failed workflow metrics, and authorized repository listing
- Public repository search
- Repository overview plus real Code/Issues/Pull Requests/Actions/Releases reads
- Platform-aware release selection for Android, Windows, Linux, iOS, and macOS assets; non-Android files are downloaded for sharing or transfer and are not run on the Android device
- Persistent clean-standard appearance controls with immediate app-wide theme updates and correct system-bar contrast
- Adaptive phone/tablet navigation, bounded wide-screen content, and pull-to-refresh dashboard feedback
- Issue metadata editing for labels, assignees, milestones, and reactions
- Workflow dispatch/cancel/rerun, logs, jobs, artifacts, and verified HTTP responses
- Android APK workflow preview, safe branch/PR creation, merged-workflow detection, dispatch, live and WorkManager-backed run tracking, completion notifications, and artifact handoff
- Repository code browsing with base64 decoding, text-file editing/creation, syntax-highlighted previews, safe rename/move/delete operations, branch-protection awareness, and reviewed branch/PR commits
- Markdown edit/preview mode with safe headings, lists, quotes, dividers, and fenced code rendering; syntax previews for Kotlin, Java, XML, JSON, YAML, and Markdown
- Recoverable fingerprinted download queue with live progress, pause/resume, confirmed cancel, retry, sharing, deletion confirmation, Room history, and APK inspection
- Own-repository CI and manual APK workflows
- Signed, versioned GitHub Release workflow with APK signature verification, pinned signing-certificate validation, APK checksums, and certificate fingerprint assets
- Actionable All GitHub hub covering 45 official website destinations, including notifications, account-wide issues and pull requests, Codespaces, Copilot, Models, Gists, Projects, organizations, enterprises, Marketplace, accessibility, security settings, billing, and community discovery

## Planned next

- Richer language grammars
- Issue templates and richer PR diff/conflict presentation
- Workflow failure annotations and dynamic `workflow_dispatch` inputs UI
- Mirror selection and Storage Access Framework download-location selection
- Richer APK permission/certificate presentation and checksum-file matching
- Biometric lock settings UI, foldable list-detail panes, complete accessibility audit, and screenshot suite
- GraphQL batching and Paging-backed large lists

These items remain visible here as roadmap work rather than being represented by fake buttons or success states.

## License

Copyright 2026 Sayanth Rock. Licensed under the Apache License 2.0. See [LICENSE](LICENSE).
