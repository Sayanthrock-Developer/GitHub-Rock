# GitHub Rock cross-platform companion

This Tauri 2 shell packages the static `site/` application as installable software:

- macOS: `.dmg` and `.pkg`
- Windows: NSIS `.exe`, `.msi`, and portable `.zip`
- Linux: `.AppImage`, `.deb`, `.rpm`, and Arch Linux `.pkg.tar.zst`
- iOS: signed `.ipa` when Apple Developer signing is configured

The companion exposes releases, documentation, project links, the offline application shell, and the live GitHub Release feed. It does not claim Android-only Kotlin functionality such as WorkManager downloads, APK inspection, Android Keystore storage, or Android package installation.

## Local desktop build

Install the official Tauri prerequisites for your operating system, then run:

```bash
cd desktop
npm ci
npm run check
npm run tauri build
```

Builds use the static frontend in `../site`. The multi-operating-system GitHub Actions workflow is the authoritative package validation because each installer must be produced on its target operating system.

## iOS

iOS builds require macOS, Xcode, Apple Developer membership, a registered bundle identifier, an Apple Distribution certificate, and a matching provisioning profile. See `docs/CROSS_PLATFORM_DISTRIBUTION.md` for the required Actions variables and secrets.
