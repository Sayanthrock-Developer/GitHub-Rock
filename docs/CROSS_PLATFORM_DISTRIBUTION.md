# Cross-platform distribution

GitHub Rock currently has two honest distribution surfaces:

1. A full native Android application for Android 10 and newer.
2. An installable web companion for macOS, Windows, Linux, iOS, iPadOS, and Android browsers.

The web companion provides the project website, live GitHub Release assets, documentation, source links, offline application-shell access, and installation guidance. It does not claim the Android app's authenticated repository, Actions, download-worker, APK-inspection, or Android Keystore features.

## Current platform matrix

| Platform | Package or installation | Current status |
| --- | --- | --- |
| Android | Signed `.apk` | Full native application |
| macOS | Browser-installed PWA | Web companion |
| Windows | Browser-installed PWA | Web companion |
| Linux | Browser-installed PWA | Web companion |
| iOS / iPadOS | Safari → Share → Add to Home Screen | Web companion |

## Installation requirements

- **macOS:** Safari 17 or newer can use **File → Add to Dock**. Chrome and Edge expose an install action in the address bar or browser menu.
- **Windows:** Chrome and Edge can install the site as a standalone app and add it to Start.
- **Linux:** Chrome, Chromium, Brave, and other Chromium-based browsers can install the site as an app. Firefox desktop does not currently provide the same PWA installation surface.
- **iOS / iPadOS:** Apple requires Safari's **Share → Add to Home Screen** flow; a website cannot trigger that confirmation automatically.
- **Android:** Chrome can install the web companion, while GitHub Releases provides the full native APK.

The website always exposes an installation button. When the browser provides a native install prompt, the button opens it. Otherwise, it opens exact instructions for the detected operating system.

## Why one Android build cannot create every native package

The current application depends on Android-only APIs and libraries, including Android components, Hilt integration, Room, DataStore, WorkManager, Android Keystore, package inspection, the system package installer, notifications, and Compose for Android. A workflow matrix can run the same source on several runners, but it cannot turn those dependencies into a signed `.dmg`, `.msix`, `.AppImage`, or `.ipa`.

iOS distribution also requires an Apple bundle identifier, an Apple Developer team, signing certificates, provisioning profiles, and App Store Connect or another permitted distribution channel. A generic unsigned IPA is not a usable public release.

## Native multiplatform roadmap

1. Extract GitHub models, API contracts, release classification, checksums, workflow policies, and validation into platform-neutral Kotlin modules.
2. Define interfaces for secure storage, persistence, background work, notifications, file access, sharing, and installation.
3. Keep Android implementations behind those interfaces while preserving the current application.
4. Add Compose Multiplatform desktop targets for macOS, Windows, and Linux.
5. Add an iOS target and replace Android-specific flows with Apple-supported equivalents.
6. Add platform-specific signing:
   - Android keystore and APK signing
   - Apple Developer ID, notarization, and iOS provisioning
   - Windows code signing
   - Linux package checksums and optional repository signatures
7. Publish only after each platform has loading, error, empty, permission, update, accessibility, and physical-device validation.

## Release asset naming contract

The Android app classifies assets from their names. Publishers should include the platform, architecture, and native extension:

```text
GitHub-Rock-1.0.0-android-arm64-v8a.apk
GitHub-Rock-1.0.0-macos-universal.dmg
GitHub-Rock-1.0.0-windows-x64.msix
GitHub-Rock-1.0.0-linux-x86_64.AppImage
GitHub-Rock-1.0.0-ios-arm64.ipa
```

Checksums and signatures should keep the full package name:

```text
GitHub-Rock-1.0.0-android-arm64-v8a.apk.sha256
GitHub-Rock-1.0.0-macos-universal.dmg.sha256
```

An asset appears for a platform only when the publisher actually uploads it. GitHub Rock does not synthesize unavailable installers or represent a source archive as a native application.
