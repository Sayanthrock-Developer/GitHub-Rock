# GitHub Rock

Everything a website can do. To the way it can be done on the mobile application. In a way that can do everything.

## GitHub Connect

You can now see when a pull request is part of a stack, check its current status, and merge stacked pull requests from wherever you are.
You can now archive your Copilot agent sessions.

### Triage notifications, review, comment, and merge, right from your mobile device

There's a lot you can do on GitHub that doesn't require a complex development environment like sharing feedback on a design discussion, or reviewing a few lines of code. GitHub Rock lets you move work forward wherever you are. Stay in touch with your team, triage issues, and even merge, right from the app. We're making these tasks easy for you to perform, no matter where you work, with a beautifully native experience.

You can use GitHub Rock to:

- Browse your latest notifications
- Read, react, and reply to Issues and Pull Requests
- Review and merge Pull Requests
- Organize Issues with labels, assignees, projects, and more
- Browse your files and code

All in one clean Kotlin + Jetpack Compose app.

> **Status:** First functional alpha  
> Real features are working. Demo mode is clearly marked. Planned features are not shown as finished.

---

## What can you do with it?

### For everyday use
- Sign in with GitHub (Device Flow – no password needed)
- Browse your repositories or search public ones
- View issues, pull requests, and Actions runs
- Download release files (APK, desktop installers, etc.)
- Inspect APK details (permissions, signing, hash)
- Switch between light / dark / true black themes

### For Android developers
- Detect Android projects automatically
- Generate safe GitHub Actions workflows for building APKs
- Create a pull request with the workflow
- Trigger builds, watch progress, and get notifications
- Download the built APK directly into the app

### Extra
- Guest mode (public repos only)
- Fully isolated Demo mode
- Quick access to 45 official GitHub.com pages (Notifications, Codespaces, Copilot, Settings, etc.)

---

## Platforms

| Platform              | What you get                                      |
|-----------------------|---------------------------------------------------|
| **Android**           | Full app (native APK)                             |
| macOS / Windows / Linux | Companion app (releases + docs)                 |
| iOS / iPadOS          | Companion app (when signed)                       |

The desktop and iOS versions are companions — they do **not** have the full Android feature set.

---

## Screenshots

*(Screenshots will be added soon after real device captures)*

| Screen        | What it shows                                      |
|---------------|----------------------------------------------------|
| Login         | Sign in, guest, and demo mode                      |
| Home          | Account status, quick actions, metrics             |
| Repositories  | Search and browse repos                            |
| Repository    | Overview, Code, Issues, PRs, Actions, Releases     |
| Builds        | Create and run Android build workflows             |
| Downloads     | Progress, pause/resume, APK inspection             |
| Profile       | Your GitHub profile + settings                     |

---

## How to build (for developers)

**Requirements**
- Android Studio
- JDK 17
- Android SDK 36

```bash
# Generate Gradle wrapper (once)
gradle wrapper --gradle-version 8.13

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew testDebugUnitTest
```

The debug APK appears in:  
`app/build/outputs/apk/debug/`

---

## Authentication setup (OAuth Device Flow)

1. Go to **GitHub → Settings → Developer settings → OAuth Apps → New OAuth App**
2. Fill in:
   - **Application name:** `Sayanth Rock Mobile Oauth`
   - **Homepage URL:** `https://github.com/Sayanthrock-Developer/GitHub-Rock`
   - **Callback URL:** `githubrock://oauth/callback`
3. Enable **Device Flow**
4. Use the public Client ID: `Ov23lim8WhLjeUMqvuMj`  
   (Never put a Client Secret in the app)

Copy `local.properties.example` → `local.properties` and set your SDK path.

---

## Architecture (simple view)

```
UI (Compose) 
    ↓
ViewModels
    ↓
Repositories
    ↓
Network (Retrofit) + Database (Room) + Preferences
```

The project uses one main `app` module with clear packages:
- `core/` – utilities
- `data/` – API, database, auth
- `ui/` – screens and theme
- `download/` – background downloads

---

## Security highlights

- Only the **public** Client ID is inside the app
- Tokens are stored encrypted with Android Keystore
- No passwords are ever requested
- Release APKs are verified against a pinned signing certificate
- Demo data is completely isolated from real accounts

See [SECURITY.md](SECURITY.md) for full details.

---

## Current status vs Planned

**Working now**
- Login (Device Flow + Guest + Demo)
- Repository browsing & search
- Issues, Pull Requests, Actions
- Android build workflow generation & monitoring
- Background downloads with progress
- APK inspection
- Theme system (light / dark / true black)

**Coming later**
- Richer code editor / language support
- Better PR diff view
- Biometric lock
- More accessibility improvements
- GraphQL + better pagination

---

## License

Copyright 2026 Sayanth Rock  
Licensed under the **Apache License 2.0** — see [LICENSE](LICENSE)
