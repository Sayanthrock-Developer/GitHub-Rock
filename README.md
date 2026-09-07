<div align="center">
  <img src="https://raw.githubusercontent.com/Sayanthrock-Developer/GitHub-Rock/main/site/assets/icon-512.png" alt="GitHub Rock" width="112" height="112" />

  # GitHub Rock

  **A native GitHub companion for Android developers.**

  Browse repositories, inspect code, follow activity, monitor GitHub Actions, manage builds, download releases and install APKs — without unnecessary website redirects.

  <p>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/releases"><img src="https://img.shields.io/github/v/release/Sayanthrock-Developer/GitHub-Rock?style=for-the-badge&label=Release" alt="Latest release" /></a>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/actions"><img src="https://img.shields.io/github/actions/workflow/status/Sayanthrock-Developer/GitHub-Rock/cross-platform-build.yml?style=for-the-badge&label=Build" alt="Build status" /></a>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/LICENSE"><img src="https://img.shields.io/github/license/Sayanthrock-Developer/GitHub-Rock?style=for-the-badge&label=License" alt="License" /></a>
  </p>
</div>

---

## Overview

GitHub Rock is a **mobile-first GitHub client for Android** built with Kotlin and Jetpack Compose.

The project focuses on real GitHub data and useful developer workflows inside the app. Features listed here are limited to functionality currently implemented in the project; planned or incomplete work is not presented as shipped functionality.

## Supported features

### GitHub account

- Sign in with GitHub Device Flow
- Secure authentication-token storage backed by Android Keystore
- Refresh and sign out of the current session
- Public browsing without signing in
- Permission-aware connected-account features

### Home

- Account overview
- Repository activity and recent repositories
- Issues and Pull Requests relevant to the account
- Build and download status
- Release/update information
- Pull-to-refresh
- Clear loading, empty and error states

### Repositories

- Search and browse GitHub repositories
- Repository creation
- Language, source/fork or visibility and sort filters
- Native repository details
- README rendering
- Files and directories
- Branch switching
- Releases
- Issues
- Pull Requests
- Commits
- GitHub Actions workflows and runs
- Repository metadata, stars, forks, topics and languages
- Direct public/private repository resolution for supported deep links

Normal repository browsing stays inside GitHub Rock. **Open on GitHub** is used only when an external GitHub page is explicitly required.

### Stars

- View starred repositories
- Repository metadata including description, language, stars, forks and update information
- Open starred repositories in the native repository experience

### Profile

- Connected GitHub profile
- Repositories
- Followers and following destinations
- Organizations
- Contributions
- Profile highlights
- Supported social links
- Pronouns when available
- ORCID detection when available

### Issues & Pull Requests

- Browse and open Issues
- View Issue state, labels, assignees and comments
- Browse Pull Requests
- Inspect changed files and diffs
- Review discussions and comments
- Reactions and reviewer information where supported
- Draft/ready state where supported
- Supported review-thread and repository actions according to GitHub permissions

### GitHub Actions & Builds

- Browse workflows and workflow runs
- Inspect jobs and steps
- Read complete workflow logs in a scrollable terminal-style view
- Inspect workflow artifacts
- Dispatch supported workflows
- Cancel supported workflow runs
- Rerun supported workflow runs
- Detect Android projects
- Generate Android debug/release workflow YAML
- Create Pull Requests for generated workflow files
- Monitor build results and artifacts

### Releases & Downloads

- Browse GitHub releases and release assets
- Classify release assets for Android, macOS, Windows, Linux and iOS when identifiable
- Download supported release assets and GitHub Actions artifacts
- Authenticated downloads for protected release/artifact requests
- Resumable background downloads
- Recovery when a server rejects a resume range
- Retry and progress handling
- Persistent download state
- APK SHA-256 fingerprinting
- Expected-checksum validation when a checksum is supplied
- APK/package/signature inspection
- Android system-installer integration for APK installation
- Installed-version comparison with available releases

### Application discovery

GitHub Rock can discover installable open-source applications using **real GitHub repository and release data**.

- Fresh and incremental repository/release discovery
- Installable entries backed by actual release assets
- Version and release information when available
- Platform and architecture-aware asset selection
- No invented repositories, packages, releases or download information

### Search & GitHub services

- Search GitHub repositories
- Search supported GitHub content
- Search and access supported Issues and Pull Requests
- Searchable GitHub services hub
- Official GitHub destinations for supported account, repository, project, package and Gist pages

### App & device information

- Package and application information
- Android API information
- Device details
- Install/update dates
- ABI information
- Requested permissions

## User interface

- Kotlin + Jetpack Compose
- Material 3 design system
- Adaptive phone, tablet and landscape layouts
- Bottom navigation on phones
- Navigation rail on larger layouts
- Light, dark and true-black appearance options
- Configurable navigation presentation
- Clear loading, empty, error, offline, permission and recovery states
- Accessible controls and readable information hierarchy
- Full-screen and lazy content presentation for large documents/logs

## Data & reliability

GitHub Rock is designed around real API responses and explicit state handling.

- GitHub REST and supported GraphQL data
- Room caching
- DataStore preferences
- WorkManager background downloads
- Android Keystore-backed authentication storage
- Redacted network logging
- SHA-256 verification for supported downloads
- No fake success states for unavailable operations
- Permission and rate-limit failures are surfaced instead of being hidden

## Architecture

```text
Compose UI
    ↓
ViewModels / UI state
    ↓
Repositories / domain logic
    ↓
GitHub API · Room · DataStore
    ↓
Auth · Actions · Builds · Downloads · APK inspection
```

### Core technologies

- Kotlin
- Jetpack Compose
- Material 3
- Android SDK
- Retrofit
- Room
- DataStore
- WorkManager
- Android Keystore
- GitHub REST APIs
- GitHub GraphQL APIs where supported
- GitHub Actions

## Android support

- **Minimum:** Android 10 / API 29
- **Maximum tested target range in the current project:** Android 16 / API 36
- **Primary platform:** Android

The repository also contains companion/web and desktop-oriented components. Their platform-specific functionality is kept separate from the native Android feature set described above.

## Build locally

### Requirements

- Android Studio
- JDK 17
- Android SDK 36
- Git

### Setup

```bash
git clone https://github.com/Sayanthrock-Developer/GitHub-Rock.git
cd GitHub-Rock
cp local.properties.example local.properties
```

Set `sdk.dir` in `local.properties`.

If the local authentication configuration requires a GitHub OAuth App client ID or backend URL, configure those values as documented in [`BUILD.md`](BUILD.md).

**Never commit tokens, client secrets, signing keys, keystores, passwords or `local.properties`.**

### Verify the project

Use the committed Gradle wrapper:

```bash
./gradlew --version
./gradlew lint
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew assembleDebugAndroidTest
```

Debug APK output:

```text
app/build/outputs/apk/debug/
```

For release signing and complete build configuration, see [`BUILD.md`](BUILD.md).

## Documentation

- [`BUILD.md`](BUILD.md) — build and release instructions
- [`ARCHITECTURE.md`](ARCHITECTURE.md) — application architecture
- [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md) — detailed implementation status and verification rules
- [`PRIVACY.md`](PRIVACY.md) — privacy information
- [`SECURITY.md`](SECURITY.md) — security reporting
- [`SUPPORT.md`](SUPPORT.md) — support guidance
- [`TERMS.md`](TERMS.md) — terms

## Development standard

GitHub Rock follows this workflow:

**Audit → Fix → Build/Test → Verify → Commit → CI → Next issue**

A feature is considered supported only when the implementation is real and the relevant workflow has been verified. Screens, buttons, API methods or roadmap documents alone are not treated as proof that a feature works.

Changes should preserve:

- Real GitHub API integration
- Correct authentication and permission handling
- Loading, success, empty, error and recovery states
- Accessibility
- Security
- Unit/UI coverage where appropriate
- CI verification
- Existing working functionality

## Project status

GitHub Rock is actively developed. This README intentionally documents the **working and relevant product surface**, while detailed implementation and roadmap information remains in [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md).

- [Releases](https://github.com/Sayanthrock-Developer/GitHub-Rock/releases)
- [Issues](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues)
- [Pull Requests](https://github.com/Sayanthrock-Developer/GitHub-Rock/pulls)

## License

Copyright © 2026 **Sayanth Rock**.

Licensed under the **Apache License 2.0**. See [`LICENSE`](LICENSE).

---

<div align="center">
  <strong>GitHub Rock</strong><br />
  <sub>GitHub, refined for mobile developers.</sub>
</div>
