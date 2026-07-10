# GitHub Rock

GitHub Rock is a native Android developer control centre for GitHub. It is designed to connect a GitHub account securely, browse repositories, generate safe Android build workflows, download and verify artifacts, inspect APK metadata, and grow into a complete mobile repository-management client.

> **Project status:** `0.1.0` is a functional foundation. The README separates implemented behavior from later milestones so unfinished operations are never presented as complete.

## Screenshots

Screenshots will be added after the first verified CI APK is installed on a test device.

## Current features

- Native Kotlin and Jetpack Compose application using Material 3.
- Five primary destinations: Home, Repositories, Builds, Downloads, and Profile.
- GitHub App Device Flow with verification code display, copy action, Custom Tabs, required polling interval, `authorization_pending`, `slow_down`, expired-code, denied-access, token refresh, and logout.
- Guest mode for public repository browsing.
- Isolated demo mode with clearly labelled mock repositories and workflows.
- Keystore-backed encrypted token persistence. Passwords, client secrets, private keys and personal access tokens are never requested or embedded.
- Central configurable `X-GitHub-Api-Version` header and redacted network logging.
- REST and GraphQL client foundations using Retrofit, OkHttp and Kotlin Serialization.
- Paging 3 repository feed for authenticated repositories or public search.
- Room cache for recently opened repositories.
- DataStore settings foundation.
- Android workflow generator for `assembleDebug`, `assembleRelease`, and `bundleRelease`. Generated YAML is displayed in full before copying.
- WorkManager HTTPS artifact download with progress, retry, cancellation, restart recovery, optional SHA-256 verification and completion notification.
- APK inspection for package name, app name, version, size, min/target SDK, permissions, signing fingerprint, SHA-256, and installed-signature comparison.
- Installation through Android's official package installer.
- Deep links for repositories, builds and releases.
- Dark/light themes, dynamic colour support, high-contrast status colors, responsive Compose layouts, empty/loading/error states.
- Unit tests for authentication response classification, repository mapping, workflow state mapping and checksum verification.
- Compose UI tests for login actions and primary navigation.
- GitHub Actions CI for unit tests, lint and debug APK assembly.

## Planned features

The following are deliberately marked as planned and are not represented by fake success states or working-looking buttons:

- Full repository overview, README, branches, tags, commits, contributors, star/watch/fork and repository creation.
- Code browser, syntax highlighting, Markdown/image/SVG previews, editing, protected-branch flow, commits and pull-request creation.
- Complete issue and pull-request management, comments, reviews, checks, conflicts and confirmed merge methods.
- Workflow run listing, logs, annotations, dispatch inputs, cancellation, reruns, artifact selection and completion notifications.
- Automatic Android project/module detection, workflow commit on a new branch, pull-request creation and verified dispatch.
- Release creation/editing, generated release notes, asset upload and confirmed deletion.
- Download pause/resume, mirror selection, duplicate history, user-selected storage and sharing.
- Biometric app lock and expanded tablet/foldable panes.

## Architecture

The project intentionally starts with one stable `app` module organized by feature:

```text
app/src/main/java/com/sayanthrock/githubrock/
├── core/
│   ├── apk/          APK inspection and package installer hand-off
│   ├── auth/         Device Flow and refresh logic
│   ├── database/     Room recent-repository cache
│   ├── download/     WorkManager artifact downloads
│   ├── model/        API and domain models
│   ├── network/      REST/GraphQL clients and headers
│   ├── preferences/  DataStore settings
│   ├── repository/   Paging and repository data access
│   ├── security/     Keystore-backed token storage
│   ├── util/         Checksum utilities
│   └── workflow/     Safe Android workflow generation
├── demo/             Isolated sample data
├── feature/          Auth, home, repositories, builds, downloads, profile
├── navigation/       Bottom navigation and deep links
└── ui/theme/         Liquid Glass-inspired design system
```

MVVM is used at screen boundaries. Hilt provides dependencies. Repositories isolate network/local data. The structure avoids unnecessary multi-module complexity while leaving clear seams for later extraction.

## GitHub App registration

1. Open GitHub **Settings → Developer settings → GitHub Apps → New GitHub App**.
2. Choose an app name and homepage URL.
3. Callback URL is not required for Device Flow.
4. Enable **Device Flow** in the GitHub App settings.
5. Install the GitHub App on the account or organizations that should be accessible.
6. Copy the public **Client ID** only. Do not place a client secret or private key in the Android application.

### Recommended GitHub App permissions

Start with the least privilege needed for the milestone you are testing:

- Metadata: Read-only (mandatory).
- Contents: Read and write.
- Issues: Read and write.
- Pull requests: Read and write.
- Actions: Read and write.
- Workflows: Read and write.
- Administration: Request only when repository creation/settings are implemented.

Organization owners can restrict installations and permissions. GitHub Rock must respect those policies and handle denied operations as errors.

## Device Flow and client ID configuration

Copy the example file:

```bash
cp local.properties.example local.properties
```

Set the Android SDK path and the public GitHub App client ID:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
GITHUB_CLIENT_ID=Iv1.your_public_client_id
GITHUB_API_VERSION=2022-11-28
```

`local.properties` is ignored by Git. Never add a client secret, private key, personal token, signing key or password.

## SDK baseline

This bootstrap uses `compileSdk = 35` and `targetSdk = 35` for a reproducible initial CI baseline. Review the current Android stable SDK and Play requirements before publishing a production release, then update the SDK deliberately with a tested dependency set.

## Build commands

Requirements: JDK 17, Android SDK 35 and Gradle 8.9.

```bash
gradle :app:testDebugUnitTest
gradle :app:lintDebug
gradle :app:assembleDebug
```

To create a standard wrapper locally:

```bash
gradle wrapper --gradle-version 8.9
./gradlew :app:assembleDebug
```

The CI workflow installs Gradle 8.9 directly, runs tests and lint, assembles the debug APK, and uploads the APK as a workflow artifact.

## Android build workflow setup

The Builds screen generates `.github/workflows/android-build.yml` for a selected application module and one of these tasks:

- `assembleDebug`
- `assembleRelease`
- `bundleRelease`

The generator only accepts sanitized module identifiers and maps build choices to fixed Gradle tasks. It does not execute arbitrary workflow input as a shell command. The generated workflow is shown completely before it can be copied. Automatic branch creation, commit, pull request and dispatch are planned for a later milestone.

## Release signing

Never store a keystore, password or signing credential in source control or in the APK.

For release builds:

1. Store the Base64 keystore and passwords as GitHub repository or environment secrets.
2. Decode the keystore only inside the GitHub Actions runner.
3. Pass secret values through environment variables.
4. Ensure logs do not print those values.
5. Delete temporary signing files before the job finishes.
6. Prefer protected GitHub environments for production signing.

GitHub Rock may verify that required secret names exist in a later milestone, but it must never read or display secret values.

## Security notes

- Access and refresh tokens are encrypted using Android Keystore-backed `EncryptedSharedPreferences`.
- Logout deletes the stored session.
- HTTP downloads are rejected; only HTTPS is accepted.
- The download client never receives the GitHub Authorization header.
- Sensitive network headers are redacted and response bodies are not logged.
- Destructive operations must require explicit confirmation when implemented.
- Branch protection must never be bypassed.
- The official Android package installer is always used; silent installation is not implemented.
- API responses are checked before success is shown.
- Demo data is isolated from connected and guest sessions.

## Licence

GitHub Rock is available under the [MIT License](LICENSE).
