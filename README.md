<div align="center">
  <img src="https://raw.githubusercontent.com/Sayanthrock-Developer/GitHub-Rock/main/site/assets/icon-512.png" alt="GitHub Rock" width="120" height="120" />

  # GitHub Rock

  **A premium, native GitHub companion for Android.**

  Browse, discover, manage, build, download, and install developer applications from GitHub through one focused mobile experience.

  <p>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/releases"><img src="https://img.shields.io/github/v/release/Sayanthrock-Developer/GitHub-Rock?style=for-the-badge&label=Release" alt="Latest release" /></a>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/actions"><img src="https://img.shields.io/github/actions/workflow/status/Sayanthrock-Developer/GitHub-Rock/cross-platform-build.yml?style=for-the-badge&label=Build" alt="Build status" /></a>
    <a href="https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/LICENSE"><img src="https://img.shields.io/github/license/Sayanthrock-Developer/GitHub-Rock?style=for-the-badge&label=License" alt="License" /></a>
  </p>
</div>

---

## ✦ Overview

**GitHub Rock** is a native, mobile-first GitHub client for Android developers. It brings GitHub discovery, repositories, profiles, Issues, Pull Requests, Actions, releases, builds, downloads, and application installation into one coherent workflow.

The project is designed around one principle:

> **Unlock the complete developer workflow without forcing users to leave the app for normal GitHub tasks.**

## ✦ All Features

### 🔐 Account & Authentication

- GitHub Device Flow authentication
- Secure token storage with Android Keystore
- Sign in and sign out
- Account/profile information
- Guest browsing for public content
- Isolated demo mode
- Account switching support
- Session and authentication-state handling

### 🏠 Home & Dashboard

- Account overview
- Repository activity
- Quick actions
- Developer metrics
- Recent repositories
- Recent Issues and Pull Requests
- Build and download status
- Release/update notifications

### 🔎 Search & Discovery

- Fast unified search
- Search repositories by name, description, owner, language, stars, forks, and topics
- Search GitHub users/owners
- Search topics
- Search Issues
- Search Pull Requests
- Filter results by type
- Recent searches
- Search suggestions
- Pagination and incremental loading

### 📦 Repository Browser

- Repository list and discovery
- Native Repository Details screen
- Owner and repository metadata
- README rendering
- File and folder browsing
- Branch selection
- Releases
- Issues
- Pull Requests
- Commits
- Actions/workflows
- Stars and forks
- Topics and languages
- Last-updated information
- Repository search and filtering
- Explicit **Open on GitHub** action when external browsing is required

### ⭐ Stars

- View starred repositories inside GitHub Rock
- Repository owner, name, description, language, stars, forks, and update time
- Native Repository Details navigation
- No unnecessary external redirects

### 👤 Profiles

- User profile
- Avatar and bio
- Followers and following
- Repositories
- Contributions/activity
- Organizations
- Achievements
- Pronouns when available
- Local time when available
- ORCID information when available
- Follow/unfollow actions where supported

### 🐛 Issues

- Browse Issues
- Issue details
- Issue comments
- Labels
- Assignees
- Open/closed state
- Create and manage Issues where permissions allow
- Native navigation from repositories

### 🔀 Pull Requests

- Browse Pull Requests
- Pull Request details
- Changed files and diffs
- Review comments
- Review threads
- Approve or request changes where permitted
- Comment and reply
- Reactions
- Resolve review threads
- Request/remove reviewers
- Draft/ready state
- Merge Pull Requests where permissions and repository rules allow

### ⚙️ GitHub Actions

- Workflow monitoring
- Workflow runs
- Job status
- Step status
- Build logs
- Failed-job reruns where permitted
- Workflow artifacts
- Build history
- Clear success/failure states

### 🏗️ Android Builds

- Detect Android projects
- Generate Android CI workflows
- Configure build workflows
- Create Pull Requests for generated workflows
- Monitor build execution
- Inspect failed jobs and logs
- Download build artifacts
- Verify generated APKs

### 📥 Downloads

- Release asset downloads
- APK artifact downloads
- Desktop installer downloads when available
- Background download handling
- Live progress
- Download state persistence
- Retry failed downloads
- Completed-download management
- Correct application icon handling

### 📱 APK & Application Installation

- Detect APK files from releases and artifacts
- Extract the APK's own application icon
- Cache extracted icons safely
- Fall back to a generic icon only when extraction fails
- Inspect APK metadata
- Inspect permissions
- Inspect signing information
- Calculate hashes
- Launch Android installation flow
- Detect installed application versions
- Compare installed version with newer releases
- Update notifications
- Open the update/download screen from notifications

### 🔄 Releases & Updates

- Browse repository releases
- Release details
- Release assets
- APK detection
- Version information
- Download assets
- New-release detection
- Installed-version comparison
- Update prompts and notifications

### 🔔 Notifications

- New release/update notifications
- Download completion notifications
- Build status notifications where supported
- Notification actions that open the relevant native screen

### 🎨 UI & Personalization

- Jetpack Compose UI
- Mobile-first layouts
- Adaptive navigation
- Light theme
- Dark theme
- True-black theme
- Developer-focused visual hierarchy
- Responsive loading states
- Clear error states
- Native dialogs and bottom sheets
- Accessibility-conscious controls

### ⚡ Reliability & UX

- Loading states for every asynchronous operation
- Empty states
- Retry actions
- Error handling
- Offline-safe cached information where available
- Pagination
- Background work
- State restoration
- No fake/mock functionality in production workflows
- Preserve existing working functionality while adding new features

## ✦ App Areas

| Area | Purpose |
|:---|:---|
| **Home** | Account status, activity, metrics, updates, and quick actions |
| **Explore** | Discover installable applications and repositories |
| **Top Charts** | Trending, just-released, and popular applications |
| **Search** | Repositories, users, topics, Issues, and Pull Requests |
| **Repositories** | Browse and manage repositories |
| **Stars** | View starred repositories natively |
| **Repository** | README, files, branches, releases, Issues, PRs, commits, and Actions |
| **Profile** | Profile, activity, followers, organizations, and account settings |
| **Issues** | Read, create, organize, label, and manage Issues |
| **Pull Requests** | Review, comment, react, resolve, and merge |
| **Actions** | Monitor workflows, jobs, steps, logs, and artifacts |
| **Builds** | Generate, run, monitor, and download Android builds |
| **Releases** | Browse releases and release assets |
| **Downloads** | Track downloads and inspect APKs |
| **Settings** | Account, appearance, notifications, downloads, and application preferences |

## ✦ Explore & App Store Experience

GitHub Rock can act as a developer-focused open-source application discovery layer on top of public GitHub releases.

### Explore

- Fresh daily feed of installable applications
- Repository/release-based application discovery
- Infinite scrolling and incremental loading
- Application icons, names, versions, descriptions, and release information
- Direct install/download actions when a compatible release asset is available

### Top Charts

- Trending applications
- Just released
- Most popular
- Ranked store-wide discovery
- Popularity based on available repository/release signals

GitHub Rock does not invent packages or releases. Installable entries must be backed by real release or artifact data.

## ✦ Native-First Experience

GitHub Rock is a **real GitHub client**, not a collection of website links.

Normal repository, profile, Issue, Pull Request, Actions, release, and download workflows stay inside the app wherever native functionality is available. External GitHub pages are reserved for actions that explicitly require opening GitHub.

**Discover → Open → Understand → Act → Build → Download → Install → Update**

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
├─────────────────────────────────┤
│ Auth · Actions · Downloads      │
└─────────────────────────────────┘
```

### Project structure

```text
app/
├── core/       Shared utilities and infrastructure
├── data/       GitHub API, authentication, database, data sources
├── ui/         Compose screens, navigation, components, theme
└── download/   Background downloads, APK handling, and artifacts
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

GitHub Rock is under active development. The README documents the complete product feature set; individual capabilities may depend on GitHub permissions, API availability, Android version, or the current application build.

For the latest implementation state, see [Releases](https://github.com/Sayanthrock-Developer/GitHub-Rock/releases), [Issues](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues), and [Pull Requests](https://github.com/Sayanthrock-Developer/GitHub-Rock/pulls).

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
