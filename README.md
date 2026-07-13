# GitHub Rock

GitHub Rock is a native Android developer control centre for GitHub. It combines secure GitHub authentication, repository browsing, issues and pull-request visibility, Actions monitoring, Android build workflow generation, verified artifact downloads, and APK inspection in one Kotlin/Compose application.

> Status: **first functional alpha**. The implemented screens and API calls are real. Demo mode is clearly labelled and isolated. Features listed under “Planned next” are intentionally not presented as complete in the app.

## Highlights

- Kotlin, Jetpack Compose, Material 3, Gradle Kotlin DSL, and a version catalog
- One stable `app` module, organized by feature and layer
- MVVM with a pragmatic Clean Architecture boundary
- Hilt, Retrofit/OkHttp, Kotlin Serialization, Room, DataStore, Paging dependencies, WorkManager, Coil, and Navigation Compose
- Android 10+ (`minSdk 29`), `compileSdk` / `targetSdk` 36
- GitHub App Device Flow with pending, slow-down, expired, denied, and refresh handling
- Guest access for public repositories and a fully isolated demo workspace
- Connected profile, API rate limit, repository search/cache foundation, workflow runs, issues, pull requests, code directory listings, and releases
- Deterministic Android project detection and safe workflow generation for `assembleDebug`, `assembleRelease`, and `bundleRelease`, followed by reviewed-branch PR creation, merged-workflow dispatch, durable run tracking, completion notifications, and artifact handoff to Downloads
- Background download worker with resume support, SHA-256 verification, retry recovery, duplicate-safe file finalization, and Room history
- APK metadata, permission, SDK, signing fingerprint, installed-signature comparison, and file hash inspection foundation
- GitHub-inspired Liquid Glass dark/light theme with dynamic color and edge-to-edge layout
- Deep links for repositories, builds, releases, and standard GitHub repository URLs
- Unit tests for authentication responses, workflow status, Android workflow generation, project detection, dispatched-run matching, completion notification policy, safe refs, and checksums
- Compose UI test for login and entry navigation

## Screenshots

| Screen | What it shows |
| --- | --- |
| Login | GitHub Device Flow, guest access, and clearly labelled demo mode |
| Home | Profile, rate limit, quick actions, recent workflows, and repositories |
| Repositories | Public/authorized repository search and repository cards |
| Repository | Overview, Code, Issues, Pull Requests, Actions, and Releases sections |
| Builds | Workflow preview/PR creation, merged-workflow detection, dispatch, live job state, background completion monitoring, and artifact handoff |
| Downloads | Verified artifact pipeline and download empty state |
| Profile | Account/session mode and token-security information |

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

## GitHub App registration and Device Flow

1. Open **GitHub Settings → Developer settings → GitHub Apps → New GitHub App**.
2. Give the app a unique name and homepage URL. A callback URL is not used by Device Flow.
3. Enable **Device Flow** in the GitHub App settings.
4. Install the GitHub App for the account or organizations the user should manage.
5. Copy the public **Client ID**. Never copy a client secret, private key, access token, keystore, or password into this project.
6. Copy `local.properties.example` to `local.properties` and configure:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
GITHUB_CLIENT_ID=Iv1.your_public_client_id
```

The value is exposed through `BuildConfig.GITHUB_CLIENT_ID`. `local.properties` is ignored by Git. The app opens GitHub in a Custom Tab, shows a copyable verification code, and polls only at GitHub's supplied interval.

## Suggested GitHub App permissions

Use the smallest permission set that matches the features you enable. Organization policy can further restrict access.

| Permission | Access | Used for |
| --- | --- | --- |
| Metadata | Read | Repository identity and basic metadata |
| Contents | Read & write | Browse files and prepare reviewed workflow/file commits |
| Issues | Read & write | Issues, comments, labels, assignees |
| Pull requests | Read & write | PR creation, review, and merge flows |
| Actions | Read & write | Runs, logs, artifacts, dispatch, cancel, rerun |
| Workflows | Read & write | Propose Android build workflow files |
| Administration | Read & write only if needed | Repository creation/settings |
| Members | Read only if needed | Assignee/reviewer resolution in organizations |

Account permissions for profile/email and starring should only be enabled when their corresponding UI is enabled. GitHub Rock never asks for a GitHub password.

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
- Release AAB → `:app:bundleRelease`

GitHub Rock's `AndroidProjectDetector` looks for `gradlew`, Gradle settings/build files, Android manifests, application-module paths, and existing workflow files. `AndroidWorkflowGenerator` maps a validated module name to a fixed Gradle task; it rejects shell syntax instead of interpolating arbitrary commands.

When adding a workflow to another repository, GitHub Rock previews the complete YAML, creates a new branch, commits the file, and opens a pull request. After that workflow is reviewed and merged, the Builds tab detects it on the repository, dispatches a selected branch or tag, identifies the new run, and follows job/step state to completion. A network-constrained WorkManager job persists run discovery across process loss and posts an honest terminal-state notification when Android notification permission is available. Published artifacts can then be queued in Downloads. Branch protection is never bypassed.

## Release signing

Create these GitHub Actions repository or environment secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

The workflow decodes the keystore into the runner's temporary directory and exposes passwords only as masked environment variables. `app/build.gradle.kts` activates the release signing configuration only when `GITHUB_ROCK_KEYSTORE_PATH` exists in the build environment. Secret values are never written into YAML, source, logs, artifacts, or the APK.

## Security

- Access and refresh tokens are kept in `EncryptedSharedPreferences` using an Android Keystore AES-256-GCM master key and are excluded from backups.
- `Authorization` and `Cookie` headers are redacted; release builds disable HTTP logging.
- The central GitHub REST version header is `BuildConfig.GITHUB_API_VERSION`.
- The network stack rejects cleartext traffic.
- APK installation is delegated to Android's official package installer; there is no silent install or security bypass.
- SHA-256 is calculated locally before an artifact is trusted.
- Destructive GitHub operations require an explicit confirmation in the product flow.
- Demo records use negative IDs, are loaded from an isolated provider, and are never merged with connected account data.
- Build notification permission is requested only when the user starts a monitored build; denying it does not block the workflow.

See [SECURITY.md](SECURITY.md) for reporting guidance.

## Current functional scope

- GitHub Device Flow session acquisition/storage/refresh/logout foundation
- Guest and isolated demo entry
- Connected dashboard request, rate limit, authorized repository listing
- Public repository search
- Repository overview plus real Code/Issues/Pull Requests/Actions/Releases reads
- Workflow dispatch/cancel/rerun, logs, jobs, artifacts, and verified HTTP responses
- Android workflow preview, safe branch/PR creation, merged-workflow detection, dispatch, live and WorkManager-backed run tracking, completion notifications, and artifact handoff
- Repository code browsing with base64 decoding, text-file editing/creation, syntax-highlighted previews, branch-protection awareness, and reviewed branch/PR commits
- Markdown edit/preview mode with safe headings, lists, quotes, dividers, and fenced code rendering; syntax previews for Kotlin, Java, XML, JSON, YAML, and Markdown
- Recoverable verified downloader and APK inspection core
- Own-repository CI and manual APK/AAB workflows

## Planned next

- Rename/move/delete operations and richer language grammars
- Issue reactions/templates, labels, milestones, assignee editing, and richer PR diff/conflict presentation
- Workflow failure annotations and dynamic `workflow_dispatch` inputs UI
- Complete download queue UI with pause/cancel/retry/mirror selection and Storage Access Framework location selection
- Richer APK permission/certificate presentation and checksum-file matching
- Release-note generation and release asset upload
- Biometric lock settings UI, tablet navigation rail, foldable list-detail panes, accessibility audit, and screenshot suite
- GraphQL batching and Paging-backed large lists

These items remain visible here as roadmap work rather than being represented by fake buttons or success states.

## License

Copyright 2026 Sayanth Rock. Licensed under the Apache License 2.0. See [LICENSE](LICENSE).
