<div align="center">

# GitHub Rock – Android GitHub Client

### GitHub, redesigned for Android.

**Native · Fast · GitHub-native · Verification-first**

Manage GitHub from your phone: browse repositories, review issues and pull requests, monitor Actions runs, build APKs, and download files with SHA-256 verification. Open source, built with Kotlin and Jetpack Compose.

> Unofficial app. Not affiliated with or endorsed by GitHub, Inc.

<p>
  <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/releases">Releases</a> ·
  <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/issues">Issues</a> ·
  <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/actions">CI</a> ·
  <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/LICENSE">Apache-2.0</a>
</p>

<p>
  <img src="https://img.shields.io/github/v/release/Sayanthrock-Developer/GitHub-Rock?style=flat-square&label=release" alt="Latest release" />
  <img src="https://img.shields.io/github/actions/workflow/status/Sayanthrock-Developer/GitHub-Rock/cross-platform-build.yml?style=flat-square&label=CI" alt="CI status" />
  <img src="https://img.shields.io/github/license/Sayanthrock-Developer/GitHub-Rock?style=flat-square" alt="License" />
</p>

</div>

---

## 01 — Project documentation

GitHub Rock's README is the **project-facing documentation entry point**. It explains what the app is, how it is built, which GitHub workflows it supports, and how problems are handled during development.

The app is intended to solve real GitHub workflow problems inside a native Android experience — not hide them behind mock data or simulated success.

### Problem-solving principle

When a problem is found, the project follows:

**Identify → Reproduce → Audit → Find the root cause → Fix → Build/Test → Verify → Commit → CI → Document**

This applies to UI/UX bugs, authentication, GitHub API integration, README rendering, search, Actions/builds, downloads, releases, networking, permissions, accessibility and other supported features.

If a problem cannot be solved because of GitHub permissions, API limits, network conditions, platform restrictions or missing backend support, GitHub Rock should show a clear actionable state instead of pretending the operation succeeded.

---

## 02 — The idea

GitHub Rock brings core GitHub workflows into a **native Jetpack Compose experience** instead of wrapping the GitHub website.

The product is built around three rules:

> **Native UI. Real GitHub data. Honest product status.**

If a capability is unavailable because of permissions, API limits, platform restrictions, or missing backend support, the app should expose that state rather than simulate success.

---

## 03 — What you can do

| Surface | GitHub Rock |
|---|---|
| **Home** | Account overview, activity, recent repositories, issues, PRs, builds, downloads and releases |
| **Repositories** | Search, browse, files, README, translation, branches, releases, issues, PRs, commits and Actions |
| **Issues** | States, labels, assignees, comments, metadata and supported actions |
| **Pull requests** | Diffs, changed files, reviews, comments, reactions and supported actions |
| **Actions / Builds** | Workflows, runs, jobs, steps, logs, artifacts, dispatch, cancellation and reruns |
| **Releases** | Releases, assets, platform classification and supported downloads |
| **Search** | Repository, content and supported issue / pull-request search |
| **Profile** | Connected GitHub account, repositories, organizations and supported profile data |

### Main navigation

**Home · Repositories · Builds · Downloads · Profile**

The primary navigation stays focused on GitHub work. Explore and Settings are not promoted to primary bottom-navigation destinations.

---

## 04 — 2026 design direction

GitHub Rock follows a **Liquid GitHub Luxury** direction:

- Dark-first charcoal surfaces
- Native Material 3 foundations
- Rounded, structured surfaces
- Subtle glass/transparency treatment where useful
- Clear typography and information hierarchy
- Responsive phone, tablet and landscape layouts
- Light, dark and true-black appearance modes
- Reduced-motion aware transitions
- Accessible controls and readable contrast
- No visual effect used as a substitute for functionality

The visual system can evolve independently from the product contract.

### README translation

Repository README content can be translated directly inside the native README viewer:

- Language picker with the supported Google ML Kit on-device translation languages
- Automatic source-language detection, including README-context fallback for short headings and labels
- Translated headings, paragraphs, bullets, tasks, quotes and supported alerts
- Code blocks, images and tables remain unchanged
- The original README is never replaced by translated content
- Model download, loading, error and retry states are surfaced honestly
- Switching back to **Original** restores the source README immediately

Translation is performed with the existing Google ML Kit integration; GitHub Rock does not use hard-coded or simulated translations.

---

## 05 — Rock Flow

Motion is treated as part of the navigation model rather than a collection of unrelated screen animations.

**Micro → Quick → Standard → Smooth**

Interactions are designed around continuity:

- Repository cards can transition into repository details
- Navigation selection can travel with the destination
- Search can expand from its toolbar context
- Build state can carry into build details
- Download progress can resolve into completion
- Account switching can preserve visual continuity

Animations should remain useful, short and predictable.

---

## 06 — Architecture

```text
┌─────────────────────────────┐
│       Jetpack Compose       │
├─────────────────────────────┤
│       ViewModels / UI       │
├─────────────────────────────┤
│     Repositories / Domain   │
├─────────────────────────────┤
│ GitHub API · Room · DataStore│
├─────────────────────────────┤
│ Auth · Actions · Releases   │
│ Downloads · Verification    │
└─────────────────────────────┘
```

### Core stack

- **Kotlin**
- **Jetpack Compose**
- **Material 3**
- **Retrofit**
- **Room**
- **DataStore**
- **WorkManager**
- **Android Keystore**
- **GitHub REST APIs**
- **GitHub GraphQL APIs where supported**
- **GitHub Actions**

The Android app reuses its authenticated networking/data layers. Feature work must not create duplicate Retrofit/API clients or parallel implementations of an existing capability.

---

## 07 — Security & reliability

GitHub Rock uses a verification-first approach to sensitive operations.

- Android Keystore-backed authentication storage
- Explicit authentication and permission states
- Redacted network logging
- SHA-256 verification where supported
- Rate-limit and network failure states
- Offline and recovery states
- No fake success for unavailable operations
- No OAuth client secret in the Android application
- No tokens, signing keys, keystores, passwords or `local.properties` in source control

See [`SECURITY.md`](SECURITY.md) and [`BUILD.md`](BUILD.md) for project-specific guidance.

---

## 08 — Android

| Requirement | Version |
|---|---|
| Minimum Android | **10 / API 29** |
| Current project range | **API 29–36** |
| Primary platform | **Android** |
| Build JDK | **17** |
| Android SDK | **36** |

The repository also contains companion/web and desktop-oriented components. Their capabilities are kept separate from the native Android product surface.

---

## 09 — Downloads & distribution

GitHub Rock follows a simple rule:

> **Only real, compatible, verifiable artifacts are documented as installable.**

Supported download flows can include GitHub release assets and GitHub Actions artifacts where the current build supports them.

The companion distribution repositories are maintained for future verified artifacts:

- [Scoop](https://github.com/Sayanthrock-Developer/GitHub-Rock-scoop-bucket)
- [WinGet](https://github.com/Sayanthrock-Developer/winget-pkgs-GitHub-Rock-)
- [Homebrew](https://github.com/Sayanthrock-Developer/homebrew-github-rock)

No guessed version, URL, checksum or platform artifact is treated as a release.

---

## 10 — Build

### Requirements

- Android Studio
- JDK 17
- Android SDK 36
- Git

### Clone

```bash
git clone https://github.com/Sayanthrock-Developer/GitHub-Rock.git
cd GitHub-Rock
cp local.properties.example local.properties
```

Configure the local SDK path and follow [`BUILD.md`](BUILD.md) for authentication and release configuration.

### Verify

```bash
./gradlew --version
./gradlew lint
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew assembleDebugAndroidTest
```

Debug artifacts are produced under:

```text
app/build/outputs/apk/debug/
```

---

## 11 — Documentation

| Document | Purpose |
|---|---|
| [`BUILD.md`](BUILD.md) | Build, configuration, signing and release |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Technical architecture |
| [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md) | Authoritative implementation state |
| [`PRIVACY.md`](PRIVACY.md) | Privacy information |
| [`SECURITY.md`](SECURITY.md) | Security reporting |
| [`SUPPORT.md`](SUPPORT.md) | Support |
| [`TERMS.md`](TERMS.md) | Terms |

---

## 12 — Development contract

GitHub Rock uses:

**Audit → Root Cause → Fix → Build/Test → Verify → Commit → CI → Next issue**

A screen, button, route, API method, mockup or roadmap entry is not proof that a capability works.

Every substantial change should preserve:

- Real GitHub integration
- Correct authentication and permissions
- Loading, success, empty, error and recovery states
- Accessibility
- Security
- Existing working functionality
- Appropriate tests
- CI verification

Detailed implementation state belongs in [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md).

---

## 13 — Project status

GitHub Rock is actively developed.

This README is the **project documentation and product-facing overview**. It intentionally avoids presenting every planned or platform-dependent capability as universally available.

For the implementation truth, use:

**[`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md)**

Useful project links:

- [Repository](https://github.com/Sayanthrock-Developer/GitHub-Rock)
- [Releases](https://github.com/Sayanthrock-Developer/GitHub-Rock/releases)
- [Issues](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues)
- [Pull requests](https://github.com/Sayanthrock-Developer/GitHub-Rock/pulls)
- [Actions](https://github.com/Sayanthrock-Developer/GitHub-Rock/actions)

---

<div align="center">

### GITHUB ROCK

**Your GitHub. Your style.**

Native Android · Real GitHub data · Built for 2026

Copyright © 2026 **Sayanth Rock**

Licensed under the **Apache License 2.0**.

</div>
