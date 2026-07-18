# Privacy Policy

_Last updated: July 18, 2026_

GitHub Rock is an open-source Android application that connects to GitHub through GitHub OAuth Device Flow. This policy explains what information the app processes and how it is handled.

## Information processed by the app

GitHub Rock may process the following information when you use connected features:

- Your GitHub OAuth access token and, when provided by GitHub, refresh token.
- GitHub account information returned by the GitHub API, such as your username, avatar, repositories, issues, pull requests, Actions runs, releases, organizations, and notifications.
- App preferences, cached repository information, download history, file checksums, and APK inspection results stored on your device.
- Files and release artifacts that you choose to download.

## How information is used

The app uses this information only to provide the features you request, including signing in, browsing and managing GitHub resources, monitoring Actions, downloading artifacts, and inspecting APK files.

GitHub Rock does not require you to enter your GitHub password inside the app. Authorization takes place on GitHub's official website in your browser.

## Local storage and security

- OAuth tokens are stored locally using Android Keystore-backed encrypted storage.
- App backup is disabled for sensitive application data.
- Authorization and cookie headers are redacted from HTTP logs, and release builds disable network logging.
- Cleartext network traffic is disabled.

Logging out clears the locally stored OAuth session. Uninstalling the app removes its local application data, subject to Android's platform behavior.

## Information shared with third parties

GitHub Rock communicates with GitHub to perform requested GitHub operations. GitHub processes information under GitHub's own terms and privacy statement.

The app may also hand actions to trusted system components or apps when you explicitly request them, including:

- Your browser for GitHub authorization and GitHub web pages.
- Android's Package Installer for APK installation.
- Android's sharing and file-opening interfaces.
- Termux, only when you explicitly use optional Termux integration.

The project owner does not receive your OAuth token through normal app operation.

## Permissions

Depending on the feature used, the Android app may request or declare permissions for internet access, network state, notifications, background work, APK installation, and optional Termux command integration. Permissions are used only for their related features.

## Support information

If you contact support or submit a GitHub issue, the information you choose to include is stored by GitHub and may be publicly visible. Never include access tokens, client secrets, private keys, passwords, keystores, or other credentials in a support request.

## Data retention and control

You can:

- Log out of GitHub Rock to clear the local OAuth session.
- Revoke the OAuth authorization from your GitHub account settings.
- Clear the app's storage from Android settings.
- Uninstall the app to remove its local data.

GitHub may retain information according to its own policies and your GitHub account settings.

## Children's privacy

GitHub Rock is a developer tool and is not directed to children. Users must meet GitHub's account-age requirements.

## Changes to this policy

Material changes will be published in this repository with an updated date.

## Contact

- Support: https://github.com/Sayanthrock-Developer/GitHub-Rock/issues
- Security reports: see [SECURITY.md](SECURITY.md)
