<div align="center">
  <img src="https://raw.githubusercontent.com/Sayanthrock-Developer/GitHub-Rock/main/site/assets/icon-512.png" alt="GitHub Rock" width="120" height="120" />

  # GitHub Rock

  **A premium, native GitHub companion for Android.**

  Browse repositories, profiles, issues, pull requests, Actions, releases, builds, downloads, and APKs from one focused mobile experience.

  <p>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/releases"><img src="https://img.shields.io/github/v/release/Sayanthrock-Developer/GitHub-Rock?style=for-the-badge&label=Release" alt="Latest release" /></a>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/actions"><img src="https://img.shields.io/github/actions/workflow/status/Sayanthrock-Developer/GitHub-Rock/cross-platform-build.yml?style=for-the-badge&label=Build" alt="Build status" /></a>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/LICENSE"><img src="https://img.shields.io/github/license/Sayanthrock-Developer/GitHub-Rock?style=for-the-badge&label=License" alt="License" /></a>
  </p>
</div>

---

## ✦ Overview

**GitHub Rock** is a native, mobile-first GitHub client built for Android developers who want to manage GitHub work without constantly switching to a browser.

The app brings repositories, profiles, Issues, Pull Requests, Actions, Android builds, releases, downloads, and APK inspection into one clean workflow.

> **Status:** Functional alpha. Core workflows are implemented and actively refined. Check Releases, Issues, and Pull Requests for the current project state.

## ✦ Highlights

- Native Android experience built with Jetpack Compose
- GitHub authentication with Device Flow
- Guest browsing for public repositories
- Native repository and profile navigation
- Issues and Pull Requests with management actions
- GitHub Actions workflow and build monitoring
- Android workflow generation and APK artifacts
- Release downloads and APK inspection
- Background download progress
- Light, dark, and true-black themes
- Secure token storage with Android Keystore

## ✦ Features

### GitHub

- Sign in with GitHub using Device Flow
- Browse public repositories without signing in
- Search repositories
- View repository files and metadata
- Browse profiles and account information
- Manage Issues, labels, and assignees
- Review, comment on, react to, and merge Pull Requests
- Monitor GitHub Actions workflows, jobs, and logs
- Browse releases and release assets

### Android Development

- Detect Android projects in repositories
- Generate Android GitHub Actions workflows
- Create Pull Requests for generated workflows
- Trigger and monitor builds
- Inspect workflow status and logs
- Download generated APK artifacts
- Inspect APK permissions, signing information, and hashes

### Downloads & Releases

- Download APKs and desktop installers from releases
- Background download handling
- Live download progress
- APK inspection
- Release-based application distribution workflows

### Personalization

- Authenticated, guest, and isolated demo modes
- Light, dark, and true-black themes
- Adaptive Android UI
- Focused developer-oriented navigation

## ✦ App Areas

| Area | Purpose |
|:---|:---|
| **Home** | Account status, activity, metrics, and quick actions |
| **Repositories** | Search, browse, and open repositories |
| **Repository** | Code, Issues, Pull Requests, Actions, Releases, and metadata |
| **Profile** | Profile, activity, and account settings |
| **Issues** | Read, organize, label, and manage issues |
| **Pull Requests** | Review, comment, react, and merge |
| **Actions** | Monitor workflows, jobs, logs, and builds |
| **Builds** | Generate and run Android build workflows |
| **Downloads** | Track downloads and inspect APKs |

## ✦ Native-First Experience

GitHub Rock is a **real GitHub client**, not a collection of website links.

Normal repository and profile browsing stays inside the app wherever native functionality is available. External GitHub pages are reserved for actions that explicitly require opening GitHub.

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

APK output:

```text
app/build/outputs/apk/debug/
```

### Unit tests

```bash
./gradlew testDebugUnitTest
```

## ✦ Authentication & Security

GitHub Rock uses **OAuth Device Flow** and does not request a GitHub password.

For local development:

1. Open **GitHub → Settings → Developer settings → OAuth Apps**.
2. Create or select an OAuth application for development.
3. Configure it according to the authentication implementation in this repository.
4. Copy `local.properties.example` to `local.properties`.
5. Add your local Android SDK configuration.

**Never commit:**

- GitHub Client Secrets
- Personal Access Tokens
- Passwords
- Private credentials
- Local configuration

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

Features may evolve during the alpha stage. See [Releases](https://github.com/Sayanthrock-Developer/GitHub-Rock/releases), [Issues](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues), and [Pull Requests](https://github.com/Sayanthrock-Developer/GitHub-Rock/pulls) for the latest state.

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
