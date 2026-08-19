<div align="center">
  <img src="https://raw.githubusercontent.com/Sayanthrock-Developer/GitHub-Rock/main/site/assets/icon-512.png" alt="GitHub Rock icon" width="128" height="128" />

  # GitHub Rock

  **A native GitHub companion for Android — built with Kotlin and Jetpack Compose.**

  Browse repositories, review issues and pull requests, monitor Actions, build Android projects, and manage GitHub work from your phone.

  <p>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/releases"><img src="https://img.shields.io/github/v/release/Sayanthrock-Developer/GitHub-Rock?style=flat-square&label=release" alt="Latest release" /></a>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/actions"><img src="https://img.shields.io/github/actions/workflow/status/Sayanthrock-Developer/GitHub-Rock/cross-platform-build.yml?style=flat-square&label=build" alt="Build status" /></a>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/LICENSE"><img src="https://img.shields.io/github/license/Sayanthrock-Developer/GitHub-Rock?style=flat-square" alt="License" /></a>
  </p>
</div>

---

## Overview

GitHub Rock is a mobile-first GitHub client designed to make common development and repository-management tasks fast and understandable on Android.

Instead of trying to reproduce every part of GitHub.com, the app focuses on the workflows that are useful when you are away from a desktop development environment.

> **Current stage:** Functional alpha. Core features are implemented, while some areas are still being refined. Demo-only and planned functionality is clearly separated from working functionality.

## What you can do

### GitHub workflow

- Sign in to GitHub with **Device Flow** — no GitHub password is entered into the app.
- Browse your repositories and search public repositories.
- View repository files and code.
- Read, react to, and reply to Issues and Pull Requests.
- Review Pull Requests and merge them when permitted.
- View GitHub Actions workflows and build progress.
- Organize Issues with labels and assignees.
- Download release assets such as APKs and desktop installers.

### Android development

GitHub Rock includes tools specifically for Android projects:

- Detect Android projects in repositories.
- Generate GitHub Actions workflows for Android builds.
- Create a Pull Request containing a generated workflow.
- Trigger and monitor builds.
- View workflow logs and build status.
- Download generated APK artifacts directly to the device.
- Inspect APK information such as permissions, signing information, and hashes.

### Modes and personalization

- **Authenticated mode** — work with your GitHub account.
- **Guest mode** — browse public repositories without signing in.
- **Demo mode** — explore the interface using isolated sample data.
- Light, dark, and true-black themes.
- Quick access to commonly used official GitHub pages.

## GitHub Connect

GitHub Rock brings several GitHub workflows into one native Android experience, including modern pull-request workflows such as stacked pull requests when the relevant GitHub data and permissions are available.

The goal is simple: **keep your GitHub work moving, even when you are away from your computer.**

## Platforms

| Platform | Status | Scope |
|---|---|---|
| **Android** | Primary platform | Full native application |
| **Windows** | Companion | Releases, documentation, and project resources |
| **macOS** | Companion | Releases, documentation, and project resources |
| **Linux** | Companion | Releases, documentation, and project resources |
| **iOS / iPadOS** | Companion / future availability | Depends on signed releases and platform support |

The **Android application is the primary full-featured client**. Companion builds should not be assumed to have feature parity with Android.

## Screens and main areas

| Area | Purpose |
|---|---|
| **Home** | Account status, activity, metrics, and quick actions |
| **Repositories** | Search and browse repositories |
| **Repository** | Code, Issues, Pull Requests, Actions, and Releases |
| **Pull Requests** | Review, comment, react, and merge |
| **Issues** | Read, organize, label, and manage issues |
| **Actions** | Monitor workflows, jobs, logs, and builds |
| **Builds** | Generate and run Android build workflows |
| **Downloads** | Track downloads and inspect APKs |
| **Profile** | GitHub profile, activity, and settings |

## Screenshots

Screenshots will be added as stable UI captures become available. The README will use real application screenshots rather than placeholder mockups.

## Architecture

GitHub Rock uses a layered Android architecture built around Jetpack Compose:

```text
┌──────────────────────────────┐
│        Compose UI            │
├──────────────────────────────┤
│         ViewModels           │
├──────────────────────────────┤
│        Repositories          │
├──────────────────────────────┤
│ API / Database / Preferences │
└──────────────────────────────┘
```

Main project areas:

```text
app/
├── core/       Shared utilities and infrastructure
├── data/       GitHub API, authentication, database, and data sources
├── ui/         Compose screens, navigation, components, and theme
└── download/   Background download and artifact handling
```

## Technology

- **Kotlin**
- **Jetpack Compose**
- **Android SDK 36**
- **JDK 17**
- **Retrofit** for networking
- **Room** for local data
- **Android Keystore** for protected token storage
- **GitHub REST APIs** for GitHub data and actions
- **GitHub Actions** for build automation

## Build locally

### Requirements

- Android Studio
- JDK 17
- Android SDK 36
- A configured Android SDK path in `local.properties`

### Build the debug APK

```bash
gradle wrapper --gradle-version 8.13
./gradlew assembleDebug
```

The generated APK is located at:

```text
app/build/outputs/apk/debug/
```

### Run unit tests

```bash
./gradlew testDebugUnitTest
```

## GitHub authentication

GitHub Rock uses the **OAuth Device Flow** so the application does not need to collect a GitHub password.

For local development:

1. Open **GitHub → Settings → Developer settings → OAuth Apps**.
2. Create or select an OAuth application for development.
3. Configure the application according to the authentication implementation in this repository.
4. Copy `local.properties.example` to `local.properties`.
5. Add your local Android SDK configuration.

> **Security:** Never add a GitHub Client Secret, personal access token, or other private credential to the Android source code or commit it to Git.

## Security

GitHub Rock is designed with several security protections:

- No GitHub passwords are requested by the app.
- OAuth tokens are protected using Android Keystore-backed storage.
- The application does not require a Client Secret to be embedded in the APK.
- Demo data is isolated from authenticated account data.
- Release APK verification can use the project's configured signing certificate information.

For security reports and responsible disclosure, see [SECURITY.md](SECURITY.md).

## Project status

### Working

- GitHub authentication and guest/demo modes
- Repository browsing and search
- Issues and Pull Requests
- GitHub Actions workflow viewing and monitoring
- Android build workflow generation
- Background downloads
- APK inspection
- Theme system

### In development

- More complete code browsing and editing capabilities
- Richer Pull Request diff experience
- Additional accessibility improvements
- More GitHub API coverage and pagination improvements

Features may change during the alpha stage. Check the repository, Releases, and project documentation for the current implementation status.

## Contributing

Contributions, bug reports, feature ideas, and UI/UX feedback are welcome.

Before opening a Pull Request:

1. Check existing Issues and Pull Requests.
2. Keep changes focused and easy to review.
3. Run the relevant tests locally.
4. Explain what changed and why.
5. Avoid committing secrets, credentials, generated local configuration, or private data.

## License

Copyright © 2026 Sayanth Rock.

GitHub Rock is licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) for the complete license text.

---

<div align="center">
  <strong>GitHub Rock</strong><br />
  Built for developers who want GitHub with them everywhere.
</div>
