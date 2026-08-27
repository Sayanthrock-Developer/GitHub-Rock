<div align="center">
  <img src="https://raw.githubusercontent.com/Sayanthrock-Developer/GitHub-Rock/main/site/assets/icon-512.png" alt="GitHub Rock" width="112" height="112" />

  # GitHub Rock

  **A native GitHub companion for Android developers.**

  Browse repositories, inspect code, follow GitHub activity, monitor Actions, build Android projects, download releases, and install APKs from one focused app.

  <p>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/releases"><img src="https://img.shields.io/github/v/release/Sayanthrock-Developer/GitHub-Rock?style=for-the-badge&label=Release" alt="Latest release" /></a>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/actions"><img src="https://img.shields.io/github/actions/workflow/status/Sayanthrock-Developer/GitHub-Rock/cross-platform-build.yml?style=for-the-badge&label=Build" alt="Build status" /></a>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/LICENSE"><img src="https://img.shields.io/github/license/Sayanthrock-Developer/GitHub-Rock?style=for-the-badge&label=License" alt="License" /></a>
  </p>
</div>

---

## Overview

GitHub Rock is a mobile-first GitHub client built around a simple idea: **keep supported GitHub workflows inside the app, make every state understandable, and never pretend an unfinished capability is complete.**

The Android application currently targets **Android 10–16 (API 29–36)**. A web companion and desktop-oriented companion shell are also included in the repository; platform-specific capabilities are labelled according to where they actually run.

## What you can do

### Account

- Sign in with GitHub Device Flow
- Securely store authentication data with Android Keystore
- Sign out and manage the current session
- Browse public GitHub content without signing in
- Use connected-account features when GitHub permissions allow them

### Home

- Account overview
- Repository activity
- Recent repositories
- Relevant Issues and Pull Requests
- Build and download status
- Release/update information
- Pull to refresh with clear loading, empty, and error states

### Repositories

- Browse and search repositories
- Open repository details without unnecessary website redirects
- Read rendered README files
- Browse files and directories
- Switch branches
- View releases, Issues, Pull Requests, commits, workflows, and repository metadata
- View stars, forks, topics, languages, and update information
- Use **Open on GitHub** only when an external page is explicitly required

### Stars

- View starred repositories
- See owner, repository name, description, language, stars, forks, and update time
- Open repositories in the native repository experience

### Search & discovery

- Search GitHub repositories
- Filter repositories by language, source/fork or visibility type, and sort order
- Search users, Issues, and Pull Requests where supported
- Discover installable applications backed by real GitHub releases

### Profiles

- View the connected user's profile
- Repositories, followers, following, organizations, and contributions
- Supported profile fields, social links, pronouns, and ORCID information when available
- Native profile and repository navigation

> Arbitrary profile search and follow actions are intentionally not presented as shipped functionality unless the current API implementation supports them.

### Issues

- Browse and open Issues
- View comments, labels, assignees, and state
- Create and manage supported Issue actions when permissions allow them

### Pull Requests

- Browse Pull Requests
- Inspect changed files and diffs
- Read and participate in review discussions where supported
- Comments, reactions, reviewers, draft/ready state, and review-thread actions
- Merge and other repository actions only when GitHub permissions and repository rules allow them

### GitHub Actions & Builds

- View workflows, runs, jobs, and steps
- Read full build logs
- Inspect artifacts
- Cancel, rerun, or dispatch supported workflows
- Detect Android projects
- Generate Android CI workflows
- Create Pull Requests for generated workflow files
- Monitor builds and download resulting artifacts

### Releases, downloads & APKs

- Browse releases and release assets
- Classify Android, macOS, Windows, Linux, and iOS assets when identifiable
- Download supported release assets and artifacts
- Resumable background downloads with progress and retry handling
- Inspect APK package information, permissions, signing information, and SHA-256 fingerprints
- Validate expected checksums when supplied
- Use the Android system installer for APK installation
- Compare installed application versions with available releases
- Support update notifications where the required information is available

### Appearance & reliability

- Material 3 / Jetpack Compose interface
- Adaptive phone, tablet, and landscape navigation
- Light, dark, and true-black appearance options
- Clear loading, empty, error, offline, permission, and recovery states
- Accessible controls and readable information hierarchy
- Cached data and background work where appropriate
- No fake production data or silent success states

## Main app areas

| Area | Purpose |
|:--|:--|
| **Home** | Account, activity, updates, builds, and downloads |
| **Explore** | Discover repositories and installable open-source applications |
| **Repositories** | Browse and manage repositories |
| **Stars** | View starred repositories |
| **Search** | Find supported GitHub content |
| **Repository** | README, files, branches, releases, Issues, PRs, commits, and Actions |
| **Profile** | Account profile, activity, repositories, and social information |
| **Builds** | Create, monitor, and inspect Android builds |
| **Downloads** | Manage downloaded releases, artifacts, and APKs |
| **Settings** | Appearance, account, notifications, downloads, and app preferences |

## Application discovery

GitHub Rock can provide an open-source application discovery experience using **real GitHub repository and release data**.

- Fresh and incremental discovery feeds
- Installable application entries backed by actual release assets
- Version, description, icon, and release information when available
- Platform and architecture-aware asset selection
- No invented packages, releases, or download information

## Platform contract

| Status | Meaning |
|:--|:--|
| **Native Android** | Implemented inside the Android application |
| **Connected GitHub** | Requires GitHub authentication and the required permission |
| **Backend-dependent** | Requires a secure service for secrets, schedules, cloud agents, or long-running jobs |
| **Companion-only** | Belongs to the web/desktop companion or operating-system integration |
| **Roadmap** | Planned work; not represented as a working feature |

The authoritative implementation details are maintained in [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md). It is intentionally separate from this README so this page stays useful to users while the implementation record remains complete.

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
Auth · Actions · Downloads · APK inspection
```

Key technologies:

- Kotlin
- Jetpack Compose / Material 3
- Android SDK 36
- JDK 17
- Retrofit
- Room
- DataStore
- Android Keystore
- GitHub REST APIs
- GitHub Actions
- WorkManager

Repository documentation:

- [`ARCHITECTURE.md`](ARCHITECTURE.md) — architecture and boundaries
- [`BUILD.md`](BUILD.md) — complete local/release build instructions
- [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md) — implementation and roadmap status
- [`PRIVACY.md`](PRIVACY.md) — privacy information
- [`SECURITY.md`](SECURITY.md) — security reporting
- [`SUPPORT.md`](SUPPORT.md) — support guidance
- [`TERMS.md`](TERMS.md) — terms

## Build locally

### Requirements

- Android Studio
- JDK 17
- Android SDK 36
- A configured GitHub OAuth App client ID when authentication configuration requires one

### Setup

```bash
cp local.properties.example local.properties
```

Set `sdk.dir` in `local.properties`. Optional local configuration can provide `GITHUB_CLIENT_ID` and `GITHUB_ROCK_BACKEND_URL` as documented in [`BUILD.md`](BUILD.md).

**Never commit tokens, client secrets, signing keys, keystores, passwords, or `local.properties`.**

### Verify

Use the committed Gradle wrapper:

```bash
./gradlew --version
./gradlew lint
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew assembleDebugAndroidTest
```

Debug APKs are generated under `app/build/outputs/apk/debug/`. Signed release builds use protected signing configuration; see [`BUILD.md`](BUILD.md).

## Security

GitHub Rock uses GitHub authentication flows rather than asking users for their GitHub password. Authentication data is protected using Android Keystore-backed storage, and sensitive values must never be committed to the repository.

For security reports, see [`SECURITY.md`](SECURITY.md).

## Development standard

A feature is not considered complete because its code compiles or its screen exists. The expected workflow is:

**Audit → Fix → Build/Test → Verify → Commit → CI → Next issue**

A supported user workflow should have:

- Real API/data integration where required
- Correct permission handling
- Loading, success, empty, error, and recovery states
- Accessibility support
- Unit/UI coverage where appropriate
- Security checks
- Passing CI evidence

When GitHub, Android, permissions, repository rules, or platform boundaries prevent an operation, GitHub Rock should explain the limitation clearly instead of presenting fake functionality.

## Project status

GitHub Rock is actively developed. Current implementation status is maintained in [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md); planned capabilities are not treated as shipped features.

**Latest:** [Releases](https://github.com/Sayanthrock-Developer/GitHub-Rock/releases)  ·  **Work:** [Issues](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues)  ·  **Code review:** [Pull Requests](https://github.com/Sayanthrock-Developer/GitHub-Rock/pulls)

## License

Copyright © 2026 **Sayanth Rock**.

Licensed under the **Apache License 2.0**. See [`LICENSE`](LICENSE).

---

<div align="center">
  <strong>GitHub Rock</strong><br />
  <sub>GitHub, refined for mobile developers.</sub>
</div>
