<div align="center">
  <img src="https://raw.githubusercontent.com/Sayanthrock-Developer/GitHub-Rock/main/site/assets/icon-512.png" alt="GitHub Rock" width="120" height="120" />

  # GitHub Rock

  **A premium, native GitHub companion for Android.**

  Browse, review, build, download, and manage your GitHub workflow — directly from your phone.

  <p>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/releases"><img src="https://img.shields.io/github/v/release/Sayanthrock-Developer/GitHub-Rock?style=for-the-badge&label=Release" alt="Latest release" /></a>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/actions"><img src="https://img.shields.io/github/actions/workflow/status/Sayanthrock-Developer/GitHub-Rock/cross-platform-build.yml?style=for-the-badge&label=Build" alt="Build status" /></a>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/LICENSE"><img src="https://img.shields.io/github/license/Sayanthrock-Developer/GitHub-Rock?style=for-the-badge&label=License" alt="License" /></a>
  </p>
</div>

---

## ✦ Overview

**GitHub Rock** is a mobile-first GitHub client built to make serious repository and development workflows feel natural on Android.

It brings repositories, profiles, Issues, Pull Requests, Actions, Android builds, releases, downloads, and APK inspection into one focused native experience.

> **Project status:** Functional alpha. Core workflows are implemented and actively refined. The README clearly separates current functionality from areas still in development.

## ✦ Why GitHub Rock?

GitHub is powerful, but mobile workflows can feel fragmented. GitHub Rock is designed around one principle:

**GitHub work should stay understandable, fast, and close at hand.**

- Native Android experience
- Clean, mobile-first navigation
- Focused developer workflows
- GitHub-powered data and actions
- Built for real repository management
- Designed for productive work away from a desktop

## ✦ Core Features

### GitHub

- Sign in with GitHub **Device Flow**
- Guest browsing for public repositories
- Search and browse repositories
- View repository files and code
- Native repository details
- Profiles and account information
- Issues with labels and assignees
- Pull Requests with review, comments, reactions, and merge support
- GitHub Actions workflow monitoring
- Releases and release assets

### Android Development

- Detect Android projects in repositories
- Generate Android GitHub Actions workflows
- Create Pull Requests containing generated workflows
- Trigger and monitor builds
- Inspect workflow status and logs
- Download generated APK artifacts
- Inspect APK permissions, signing information, and hashes

### Downloads & Releases

- Download APKs and desktop installers from releases
- Background download handling
- Download progress tracking
- APK inspection
- Release-based application distribution workflows

### Personalization

- Authenticated mode
- Guest mode
- Isolated demo mode
- Light, dark, and true-black themes
- Clean, adaptive Android UI

## ✦ Main Areas

| Area | Purpose |
|:---|:---|
| **Home** | Account status, activity, metrics, and quick actions |
| **Repositories** | Search, browse, and open repositories |
| **Repository** | Code, Issues, Pull Requests, Actions, Releases, and metadata |
| **Profile** | GitHub profile, activity, and account settings |
| **Issues** | Read, organize, label, and manage issues |
| **Pull Requests** | Review, comment, react, and merge |
| **Actions** | Monitor workflows, jobs, logs, and builds |
| **Builds** | Generate and run Android build workflows |
| **Downloads** | Track downloads and inspect APKs |

## ✦ Native-first Experience

GitHub Rock is designed as a **real GitHub client**, not a collection of website links.

Normal repository and profile workflows are intended to remain inside the application wherever the native experience supports them. External GitHub pages are reserved for actions that explicitly require opening GitHub.

**Discover → Open → Understand → Act → Verify**

## ✦ Architecture

GitHub Rock uses a layered Android architecture centered around Jetpack Compose.

```text
┌─────────────────────────────────┐
│            Compose UI           │
├─────────────────────────────────┤
│           ViewModels            │
├─────────────────────────────────┤
│          Repositories           │
├─────────────────────────────────┤
│ API · Database · Preferences    │
└─────────────────────────────────┘
```

### Project structure

```text
app/
├── core/       Shared utilities and infrastructure
├── data/       GitHub API, authentication, database, data sources
├── ui/         Compose screens, navigation, components, theme
└── download/   Background downloads and artifact handling
```

## ✦ Technology

- **Kotlin**
- **Jetpack Compose**
- **Android SDK 36**
- **JDK 17**
- **Retrofit**
- **Room**
- **Android Keystore**
- **GitHub REST APIs**
- **GitHub Actions**

## ✦ Build Locally

### Requirements

- Android Studio
- JDK 17
- Android SDK 36
- Android SDK path configured in `local.properties`

### Debug APK

```bash
./gradlew assembleDebug
```

The APK is generated at:

```text
app/build/outputs/apk/debug/
```

### Unit tests

```bash
./gradlew testDebugUnitTest
```

## ✦ Authentication

GitHub Rock uses **OAuth Device Flow**, so the application does not collect a GitHub password.

For local development:

1. Open **GitHub → Settings → Developer settings → OAuth Apps**.
2. Create or select an OAuth application for development.
3. Configure it according to the authentication implementation in this repository.
4. Copy `local.properties.example` to `local.properties`.
5. Add your local Android SDK configuration.

> **Security:** Never commit GitHub Client Secrets, personal access tokens, private credentials, or local configuration.

## ✦ Security

- GitHub passwords are never requested by the app.
- OAuth tokens use Android Keystore-backed protected storage.
- Client Secrets are not embedded in the Android application.
- Demo data remains isolated from authenticated account data.
- APK verification can use configured signing certificate information.

Security reports and responsible disclosure: [SECURITY.md](SECURITY.md)

## ✦ Project Status

### Working

- GitHub authentication
- Guest and demo modes
- Repository browsing and search
- Issues and Pull Requests
- GitHub Actions monitoring
- Android build workflow generation
- Background downloads
- APK inspection
- Theme system

### In Development

- More complete code browsing and editing
- Richer Pull Request diff experience
- Additional accessibility improvements
- Broader GitHub API coverage
- Further pagination improvements

Features may evolve during the alpha stage. See [Releases](https://github.com/Sayanthrock-Developer/GitHub-Rock/releases), Issues, and Pull Requests for the latest project state.

## ✦ Contributing

Contributions, bug reports, feature requests, and UI/UX feedback are welcome.

Before opening a Pull Request:

1. Check existing Issues and Pull Requests.
2. Keep the change focused and reviewable.
3. Run the relevant tests locally.
4. Explain what changed and why.
5. Never commit secrets, credentials, generated local configuration, or private data.

## ✦ License

Copyright © 2026 **Sayanth Rock**.

GitHub Rock is licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) for the complete license text.

---

<div align="center">
  <strong>GitHub Rock</strong>
  <br />
  <sub>GitHub, refined for mobile developers.</sub>
  <br /><br />
  <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/releases">Releases</a>
  ·
  <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/issues">Issues</a>
  ·
  <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/pulls">Pull Requests</a>
</div>
