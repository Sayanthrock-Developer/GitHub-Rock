# 🔐 GitHub Rock authentication and APK signing

This guide matches the production architecture used by GitHub Rock.

GitHub Rock is a native Android application, so it uses **GitHub App Device Flow**. Device Flow needs only a public Client ID. It does not use an OAuth callback URL, client secret, private key, or embedded access token.

## TL;DR

1. Create a GitHub App.
2. Enable Device Flow.
3. Copy the public Client ID.
4. Put it in `local.properties` for local builds, or in the `PUBLIC_GITHUB_CLIENT_ID` Actions variable for fork CI builds.
5. Sync and run the project.
6. Keep every secret, keystore, password, private key, and token outside the repository and APK.

---

## 🔑 GitHub authentication configuration

<details>
<summary><strong>Show full setup guide</strong></summary>

### 1 — Create a GitHub App

Open:

**GitHub → Settings → Developer settings → GitHub Apps → New GitHub App**

Use values similar to these:

| Field | Value |
| --- | --- |
| GitHub App name | `GitHub Rock Development` or another globally unique name |
| Homepage URL | `https://github.com/Sayanthrock-Developer/GitHub-Rock` |
| Callback URL | Not required for Device Flow |
| Webhook | Disable it unless your own backend needs one |

After the app is created:

1. Open the app settings.
2. Enable **Device Flow**.
3. Configure only the repository and account permissions used by the app.
4. Install the GitHub App on the account or organization that should be managed.
5. Copy the public **Client ID**.

> GitHub Rock never needs a Client Secret for Device Flow. Never put a Client Secret, private key, token, password, or keystore inside an Android project or APK.

### 2 — Add the Client ID locally

Copy `local.properties.example` to `local.properties` in the repository root:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
GITHUB_CLIENT_ID=YOUR_PUBLIC_GITHUB_APP_CLIENT_ID
```

`local.properties` is ignored by Git.

The app module already exposes this value through:

```kotlin
BuildConfig.GITHUB_CLIENT_ID
```

The official release build also has a bundled public Client ID. Forks can override it in GitHub Actions with:

**Repository → Settings → Secrets and variables → Actions → Variables → New repository variable**

```text
Name: PUBLIC_GITHUB_CLIENT_ID
Value: YOUR_PUBLIC_GITHUB_APP_CLIENT_ID
```

A Client ID is public. It is safe to ship in the APK. Secrets are not.

### 3 — Sync and run

Use Android Studio with JDK 17 and Android SDK 36, then run:

```bash
gradle wrapper --gradle-version 8.13
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

The debug APK is generated under:

```text
app/build/outputs/apk/debug/
```

### 4 — How sign-in works

1. GitHub Rock requests a device code from GitHub.
2. The app displays the verification code.
3. GitHub opens in an Android Custom Tab.
4. The user authorizes the GitHub App on GitHub's official page.
5. GitHub Rock polls only at GitHub's supplied interval.
6. Access and refresh tokens are stored with Android Keystore-backed encryption.

No embedded WebView password form is used. GitHub Rock never asks for a GitHub password.

### 5 — Why there is no `githubrock://oauth/callback`

The current implementation uses Device Flow, which does not redirect back through an OAuth callback URL. Adding a callback intent filter without implementing a complete, supported token-exchange flow would create dead code and could mislead contributors.

The existing `githubrock://` deep-link handling is reserved for in-app navigation such as repository, build, and release destinations.

</details>

---

## Suggested GitHub App permissions

Grant the smallest permission set that matches enabled features.

| Permission | Access | Purpose |
| --- | --- | --- |
| Metadata | Read | Repository identity and metadata |
| Contents | Read & write | Browse and prepare reviewed file/workflow changes |
| Issues | Read & write | Issues, comments, labels, and assignees |
| Pull requests | Read & write | Pull-request creation, review, and merge flows |
| Actions | Read & write | Runs, logs, artifacts, dispatch, cancel, and rerun |
| Workflows | Read & write | Propose Android workflow files |
| Administration | Only when needed | Repository creation or settings |
| Members | Read only when needed | Organization assignee/reviewer resolution |

After changing account permissions, save the GitHub App settings and re-authorize the user. Repository or organization permission changes may require approval from the installation owner.

---

## 🔏 APK signing certificate

Official releases are signed in GitHub Actions. The release workflow verifies the APK with Android `apksigner` and publishes two separate SHA-256 files:

| Release asset | Meaning |
| --- | --- |
| `GitHub-Rock-<version>.apk.sha256` | SHA-256 checksum of the APK file |
| `GitHub-Rock-<version>.apk.certificate.sha256` | SHA-256 fingerprint of the APK signing certificate |

Do not confuse the APK file checksum with the signing-certificate fingerprint.

### Read the signing certificate from an APK

```bash
apksigner verify --verbose --print-certs GitHub-Rock-<version>.apk
```

Look for:

```text
Signer #1 certificate SHA-256 digest: ...
```

### Read the certificate from a keystore

```bash
keytool -list -v \
  -keystore release.jks \
  -alias your_key_alias
```

Publish only the public SHA-256 certificate fingerprint. Never publish the keystore, private key, alias password, keystore password, or signing password.

### Required GitHub Actions signing secrets

Create these under:

**Repository → Settings → Secrets and variables → Actions → Secrets**

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

The workflow decodes the keystore only inside the temporary GitHub Actions runner directory, validates it, signs the APK, verifies the signer certificate, generates checksums, uploads the workflow artifact, and publishes the release assets.

---

## Security checklist

- [x] Public Client ID only in the APK
- [x] No Client Secret in source, Gradle files, resources, or APK
- [x] `local.properties`, `keystore.properties`, `*.jks`, and `*.keystore` ignored by Git
- [x] GitHub login through official GitHub pages
- [x] Device Flow polling follows GitHub's supplied interval
- [x] Tokens stored with Android Keystore-backed encryption
- [x] Signed release APK verified with `apksigner`
- [x] APK checksum and certificate fingerprint published separately
