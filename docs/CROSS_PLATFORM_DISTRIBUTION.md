# Cross-platform distribution

GitHub Rock has a full native Android application and a packaged cross-platform companion. The companion uses Tauri 2 to put the project website, live GitHub Release files, documentation, and project links into an installable application window. It does not represent Android-only repository management, WorkManager, Keystore, APK inspection, or package-installer features as desktop or iOS functionality.

## Release package matrix

The release workflow builds and verifies every required desktop package before it creates a public GitHub Release.

| Platform | Architectures | Release files | Installation |
| --- | --- | --- | --- |
| Android 10+ | Android package ABIs | `GitHub-Rock-<version>.apk` | Android Package Installer |
| macOS 11+ | Apple Silicon (`arm64`) and Intel (`x64`) | `.dmg` and `.pkg` | Drag from the DMG or run the installer package |
| Windows 10/11 | `x64` | `.msi`, setup `.exe`, and portable `.zip` | Windows Installer, setup wizard, or unzip and run |
| Linux | `x64` | `.AppImage`, `.deb`, `.rpm`, and `.pkg.tar.zst` | AppImage or the distribution's package manager |
| iOS / iPadOS 15+ | `arm64` | Signed `.ipa` when Apple signing is enabled | TestFlight, App Store, or another Apple-permitted distribution route |

Each package also has a same-name `.sha256` file. The Android release additionally includes the independently reviewed signing-certificate fingerprint.

The GitHub Pages progressive web app remains available as a no-download fallback on macOS, Windows, Linux, iOS, iPadOS, and Android.

## Install the packages

### macOS

Choose the `arm64` asset for Apple Silicon Macs and `x64` for Intel Macs. Open the `.dmg` and copy GitHub Rock to Applications, or run the matching `.pkg`.

A package built without an Apple Developer ID signature can still be produced, but macOS Gatekeeper may require an explicit approval in Privacy & Security. Production releases should be signed and notarized with an Apple Developer ID certificate.

### Windows

Use the `.msi` for Windows Installer, the `-setup.exe` for the guided installer, or the portable `.zip` when installation is not desired. The package targets 64-bit Windows.

Unsigned Windows applications can run after the user acknowledges Microsoft SmartScreen. A production publisher should configure a Windows code-signing certificate to remove avoidable trust warnings.

### Linux

- Make the `.AppImage` executable and run it.
- Install the `.deb` on Debian or Ubuntu.
- Install the `.rpm` on Fedora, RHEL, or a compatible distribution.
- Install the `.pkg.tar.zst` on Arch Linux or a compatible distribution.

The Linux packages target `x86_64` and require the WebKitGTK runtime supplied by the distribution.

### iOS and iPadOS

iOS does not permit a universal unsigned GitHub Release download that installs on every device. The workflow therefore builds an IPA only when valid Apple Developer signing material is configured. That IPA must then be distributed through TestFlight, the App Store, or another route Apple permits for the publisher and users.

Configure these Actions values before setting `ENABLE_IOS_RELEASE` to `true`:

| Type | Name | Value |
| --- | --- | --- |
| Variable | `ENABLE_IOS_RELEASE` | `true` |
| Variable | `APPLE_TEAM_ID` | Apple Developer team identifier |
| Secret | `APPLE_IOS_CERTIFICATE_BASE64` | Base64 Apple Distribution `.p12` |
| Secret | `APPLE_IOS_CERTIFICATE_PASSWORD` | Password for the `.p12` |
| Secret | `APPLE_IOS_PROVISIONING_PROFILE_BASE64` | Base64 App Store Connect provisioning profile |

The registered Apple App ID and provisioning profile must match `com.sayanthrock.githubrock.companion`.

## Build workflows

- `.github/workflows/cross-platform-build.yml` validates the Tauri configuration on pull requests and builds macOS, Windows, and Linux packages. Its manual dispatch can also build the signed iOS IPA.
- `.github/workflows/release.yml` first verifies the signed Android APK, calls the cross-platform build, checks the complete asset matrix, and only then publishes one GitHub Release.
- `tools/release/collect-desktop-artifacts.mjs` gives generated installers predictable release names.
- `tools/release/write-checksums.mjs` creates SHA-256 sidecars.
- `tools/release/package-arch.sh` creates the Arch Linux package.

The desktop companion can be checked locally with:

```bash
npm ci --prefix desktop
npm run check --prefix desktop
```

## Why the platform scopes differ

The Android application depends on Android components, Hilt, Room, DataStore, WorkManager, Android Keystore, package inspection, notifications, and the Android package installer. Tauri packages the platform-neutral website as a real desktop or iOS application, but it does not convert those Android APIs into equivalents on other operating systems.

A later feature-parity migration would still need portable domain modules plus platform implementations for secure storage, persistence, background work, notifications, files, installation, and code signing. Until then, release descriptions and the UI must distinguish the full Android control centre from the cross-platform companion.
