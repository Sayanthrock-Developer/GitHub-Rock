# GitHub Rock

GitHub Rock is a premium native Android developer control centre for GitHub. It is designed to manage repositories, issues, pull requests, Actions workflows, Android cloud builds, releases, downloads and APK inspection from a phone without bypassing GitHub or Android security.

## Current first version

This branch contains a buildable single-module Android foundation with:

- Kotlin, Jetpack Compose and Material 3
- Five-tab navigation: Home, Repositories, Builds, Downloads and Profile
- Premium GitHub-inspired dark Liquid Glass visual system
- Clearly labelled isolated demo data
- GitHub Device Flow request/response models and state handling
- Central GitHub API version configuration
- Retrofit, OkHttp and Kotlin Serialization dependencies
- Hilt application setup
- Room, DataStore, Paging and WorkManager dependencies ready for feature implementation
- SHA-256 file verification utility with unit test
- Deep-link declarations for repository, build and release routes
- CI workflow for unit tests, lint and debug APK builds

Advanced write operations are intentionally not presented as complete until their API, permission, confirmation and error-handling paths are implemented and tested.

## Screenshots

Add phone, tablet and foldable screenshots here after the first CI-built APK is installed and reviewed.

## Architecture

One stable `app` module organised by feature, following MVVM and pragmatic Clean Architecture boundaries:

- `ui`: Compose screens, navigation and reusable design components
- `data`: GitHub REST/OAuth DTOs and API contracts
- `security`: checksum and future Keystore-backed credential storage
- future feature packages: auth, repositories, code, issues, pullrequests, actions, builds, downloads, releases and profile

The project deliberately avoids premature multi-module complexity.

## GitHub App and Device Flow setup

1. Open GitHub Developer Settings and create a GitHub App.
2. Enable Device Flow for the app.
3. Configure only the permissions needed by the features you enable.
4. Put the public client ID in your untracked `local.properties` file:

```properties
GITHUB_CLIENT_ID=your_public_client_id
```

Never place a client secret, private key, access token, signing key or repository secret in the APK or repository.

## Suggested GitHub App permissions

Start read-only and add write access only when needed:

- Metadata: read
- Contents: read; write only for explicit file/workflow commits
- Issues: read/write
- Pull requests: read/write
- Actions: read/write for dispatch, rerun and cancellation
- Workflows: write only when the user explicitly commits a generated workflow
- Administration: only if repository creation/settings features require it

Organisation policy can restrict any permission regardless of app configuration.

## Build

Requirements:

- JDK 17
- Android SDK 34
- Gradle 8.7 or Android Studio with compatible Gradle support

Commands:

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
```

The CI workflow installs Gradle 8.7, so a wrapper JAR is not committed in this initial branch.

## Android build workflow design

The planned builder will:

1. Detect Gradle and Android application modules.
2. Select branch or tag and build type.
3. Reuse a compatible existing workflow when possible.
4. Show generated workflow YAML before any commit.
5. Commit new workflow files only on a new branch.
6. Offer a pull request.
7. Dispatch GitHub Actions and track jobs, steps, logs and artifacts.
8. Verify downloaded artifacts before installation.

Supported tasks: `assembleDebug`, `assembleRelease`, and `bundleRelease`.

## Release signing

Release signing credentials must be stored in GitHub repository or environment secrets. Workflow files may reference secret names, but must never contain secret values. The Android app must never download or display signing credentials.

## Security

- Tokens will use Android Keystore-backed encryption.
- Sensitive headers and bodies must be redacted from logs.
- Destructive actions require explicit confirmation.
- Branch protection is never bypassed.
- Workflow input is treated as data and never interpolated into unsanitised shell commands.
- APK installation uses Android's official package installer.
- Demo data is isolated from authenticated account data.

## Planned features

- Complete Device Flow polling and secure token persistence
- Live repository lists with Paging and Room caching
- Repository detail tabs and code browser
- File editing through protected branches and pull requests
- Issues and pull request workflows
- Actions runs, logs, reruns, cancellation and notifications
- Android project detection and workflow generation
- Recoverable WorkManager download queue
- APK metadata, permissions and signing-certificate inspection
- Release creation and asset upload
- Biometric app lock
- Tablet and foldable adaptive layouts
- Expanded unit and Compose UI test suites

## Licence

No licence has been selected yet. Add a `LICENSE` file before public redistribution or accepting external contributions.
