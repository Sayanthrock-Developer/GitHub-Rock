<div align="center">
  <img src="https://raw.githubusercontent.com/Sayanthrock-Developer/GitHub-Rock/main/site/assets/icon-512.png" alt="GitHub Rock" width="112" height="112" />

  # GitHub Rock

  **A native, mobile-first GitHub client for Android developers.**

  Browse repositories, inspect code, manage issues and pull requests, monitor GitHub Actions, explore releases, and handle supported downloads through a native Android experience built around real GitHub data.

  <p>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/releases"><img src="https://img.shields.io/github/v/release/Sayanthrock-Developer/GitHub-Rock?style=for-the-badge&label=Release" alt="Latest release" /></a>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/actions"><img src="https://img.shields.io/github/actions/workflow/status/Sayanthrock-Developer/GitHub-Rock/cross-platform-build.yml?style=for-the-badge&label=Build" alt="Build status" /></a>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/LICENSE"><img src="https://img.shields.io/github/license/Sayanthrock-Developer/GitHub-Rock?style=for-the-badge&label=License" alt="License" /></a>
  </p>
</div>

---

## Overview

GitHub Rock is a **native Android GitHub companion** built with Kotlin and Jetpack Compose.

### Project principles

- **Native first** — GitHub workflows should feel like an Android application, not a website wrapper.
- **Real data** — product claims are backed by actual GitHub integrations and explicit state handling.
- **Honest status** — unfinished or permission-dependent capabilities are not presented as working merely because a screen, button, route, or API method exists.

The authoritative implementation state is maintained in [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md).

## Features

### Accounts & profile

- GitHub Device Flow authentication foundation
- Android Keystore-backed token protection
- Session refresh and sign-out
- Public browsing without authentication
- Connected GitHub profile
- Repositories, followers, following, organizations, contributions, and supported profile fields
- Permission-aware account features

### Home

- Account overview
- Repository activity and recent repositories
- Relevant issues and pull requests
- Build/download information
- Release/update information
- Pull-to-refresh
- Loading, empty, error, offline, and recovery states

### Repositories

- Repository search and browsing
- Repository creation
- Language, source/fork or visibility, and sort filters
- Native repository details
- README rendering
- Files and directories
- Branch switching
- Releases, issues, pull requests, and commits
- GitHub Actions workflows and runs
- Metadata, stars, forks, topics, and languages
- Supported public/private repository deep-link resolution

Repository browsing stays inside GitHub Rock whenever the required GitHub data is available. **Open on GitHub** is reserved for destinations that explicitly require the external website.

### Issues & Pull Requests

- Browse issues and pull requests
- States, labels, assignees, comments, and supported metadata
- Changed files and diffs
- Review discussions and comments
- Reactions and reviewer information where supported
- Draft/ready state where supported
- Supported review-thread and repository actions according to GitHub permissions

### GitHub Actions & Builds

- Workflows and workflow runs
- Jobs and steps
- Scrollable terminal-style workflow logs
- Workflow artifacts
- Supported workflow dispatch, cancellation, and reruns
- Android project detection
- Supported debug/release workflow YAML generation
- Pull requests for generated workflow files
- Build and artifact monitoring

### Releases & downloads

- GitHub releases and release assets
- Android, macOS, Windows, Linux, and iOS asset classification where identifiable
- Supported release-asset and GitHub Actions artifact downloads
- Authenticated GitHub downloads where required
- Download progress and recovery states
- SHA-256 fingerprinting and supplied-checksum validation
- Android APK/package/signature inspection foundation
- Android system-installer integration for APK installation

Download behavior depends on GitHub permissions, asset availability, Android package rules, and the capability of the current build.

### Application discovery

Discover installable open-source applications from **real GitHub repository and release data**.

- Repository/release discovery
- Release-asset-backed entries
- Version and release information when available
- Platform and architecture-aware asset selection
- No invented repositories, packages, releases, or download information

### Search & GitHub services

- GitHub repository search
- Supported content search
- Supported issue and pull-request search
- Searchable GitHub services hub
- Official GitHub destinations for supported account, repository, project, package, and Gist pages

### App & device information

- Package/application information
- Android API information
- Device details
- Installation/update dates
- ABI information
- Requested permissions

---

## UI & UX

GitHub Rock uses a modern native Android design language focused on clarity, productivity, and accessibility.

- Kotlin + Jetpack Compose
- Material 3 foundation
- Mobile-first responsive layouts
- Adaptive phone, tablet, and landscape layouts
- Bottom navigation on phones
- Navigation rail on larger layouts
- Light, dark, and true-black appearance options
- Configurable navigation presentation
- Rounded structured surfaces with restrained visual effects
- Strong information hierarchy for repositories, builds, releases, and accounts
- Accessible controls and readable typography
- Efficient full-screen/lazy presentation for large documents and logs
- Explicit loading, empty, error, offline, permission, and recovery states

Visual styling can evolve without changing the product contract. UI changes must not introduce fake functionality or duplicate data/API systems.

---

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
Auth · Actions · Releases · Downloads · APK inspection
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

The Android app uses the existing authenticated networking/data layers rather than introducing duplicate Retrofit/API infrastructure for individual features.

---

## Data, security & reliability

- GitHub REST and supported GraphQL data
- Room caching
- DataStore preferences
- WorkManager for supported background work
- Android Keystore-backed authentication storage
- Redacted network logging
- SHA-256 verification for supported downloads
- Permission-aware API operations
- Rate-limit and failure states surfaced to users
- No fake success states for unavailable operations
- No tokens, client secrets, signing keys, keystores, passwords, or `local.properties` committed to the repository

Security-sensitive credentials must follow [`BUILD.md`](BUILD.md) and remain outside the Android source tree.

---

## Android support

- **Minimum:** Android 10 / API 29
- **Current project target range:** API 29–36
- **Primary platform:** Android

The repository also contains companion/web and desktop-oriented components. Their platform-specific functionality is kept separate from the native Android feature set described here.

---

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

Set the correct `sdk.dir` in `local.properties`. If authentication requires a GitHub OAuth App client ID or backend URL, follow [`BUILD.md`](BUILD.md).

> **Never commit** tokens, client secrets, signing keys, keystores, passwords, or `local.properties`.

### Verify locally

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

---

## Documentation

| Document | Purpose |
|---|---|
| [`BUILD.md`](BUILD.md) | Build, configuration, signing, and release instructions |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Application architecture and technical structure |
| [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md) | Authoritative implementation status and verification rules |
| [`PRIVACY.md`](PRIVACY.md) | Privacy information |
| [`SECURITY.md`](SECURITY.md) | Security reporting |
| [`SUPPORT.md`](SUPPORT.md) | Support guidance |
| [`TERMS.md`](TERMS.md) | Terms |

---

## Development standard

GitHub Rock follows a verification-first workflow:

**Audit → Root Cause → Fix → Build/Test → Verify → Commit → CI → Next issue**

A capability is supported only when the implementation is real and relevant verification evidence exists.

Screens, buttons, navigation routes, API methods, roadmap documents, and design mockups are **not** proof that a feature works.

Every substantial change should preserve:

- Real GitHub API integration
- Correct authentication and permission handling
- Appropriate loading, success, empty, error, offline, and recovery states
- Accessibility
- Security
- Unit/UI coverage where appropriate
- CI verification
- Existing working functionality

Platform, permission, backend, or OS dependencies must remain visible in the implementation and documentation.

---

## Project status

GitHub Rock is actively developed. This README documents the product surface and development contract; [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md) is the authoritative source for detailed implementation status.

- [Releases](https://github.com/Sayanthrock-Developer/GitHub-Rock/releases)
- [Issues](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues)
- [Pull Requests](https://github.com/Sayanthrock-Developer/GitHub-Rock/pulls)
- [Actions](https://github.com/Sayanthrock-Developer/GitHub-Rock/actions)

---

## License

Copyright © 2026 **Sayanth Rock**.

Licensed under the **Apache License 2.0**. See [`LICENSE`](LICENSE).

---

<div align="center">
  <strong>GitHub Rock</strong><br />
  <sub>GitHub, refined for mobile developers.</sub>
</div>
