# 🔐 GitHub Rock authentication and APK signing

This guide matches the production architecture used by GitHub Rock.

GitHub Rock is a native Android application, so it uses **GitHub App Device Flow**. Device Flow needs only a public Client ID. It does not use an OAuth callback URL, client secret, private key, or embedded access token.

## TL;DR

1. Create a GitHub App.
2. Enable Device Flow.
3. Copy the public Client ID.
4. Put it in `local.properties` for local builds, or in the `PUBLIC_GITHUB_CLIENT_ID` Actions variable for fork CI builds.
5. Sync and run the project.
6. Before publishing a release, independently read the release-key SHA-256 fingerprint and set `EXPECTED_RELEASE_CERT_SHA256` in GitHub Actions variables.
7. Keep every secret, keystore, password, private key, and token outside the repository and APK.

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

Official releases are signed in GitHub Actions. The release workflow verifies the APK with Android `apksigner`, compares the detected signer certificate with a separately configured trusted fingerprint, and only then publishes release assets.

### Pin the trusted release certificate

The repository owner must independently read the fingerprint from the intended release keystore before the first release:

```bash
keytool -list -v \
  -keystore release.jks \
  -alias your_key_alias
```

Copy the certificate's SHA-256 fingerprint, then create this GitHub Actions repository variable:

**Repository → Settings → Secrets and variables → Actions → Variables**

```text
Name: EXPECTED_RELEASE_CERT_SHA256
Value: YOUR_TRUSTED_64_HEX_SHA256_FINGERPRINT
```

Colons and letter case are accepted; the workflow normalizes the value before comparison. The release fails closed when the variable is missing, malformed, or does not match the APK signer.

> Owner action required: the exact official fingerprint cannot be generated from source code because the private release keystore is intentionally not stored in this repository. Set `EXPECTED_RELEASE_CERT_SHA256` from the independently controlled release keystore before running the release workflow.

### Published release assets

After the pinned certificate check succeeds, the workflow publishes two separate SHA-256 files:

| Release asset | Meaning |
| --- | --- |
| `GitHub-Rock-<version>.apk.sha256` | SHA-256 checksum of the APK file |
| `GitHub-Rock-<version>.apk.certificate.sha256` | SHA-256 fingerprint detected from the APK signing certificate |

Do not confuse the APK file checksum with the signing-certificate fingerprint. The certificate asset is a convenient copy of the detected value, not the root of trust. Users should compare it with the fingerprint previously published through an independently trusted project channel, such as a reviewed security document or the organization's official website.

### Read the signing certificate from an APK

```bash
apksigner verify --verbose --print-certs GitHub-Rock-<version>.apk
```

Look for:

```text
Signer #1 certificate SHA-256 digest: ...
```

### Required GitHub Actions signing secrets

Create these under:

**Repository → Settings → Secrets and variables → Actions → Secrets**

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

The workflow decodes the keystore only inside the temporary GitHub Actions runner directory, validates it, signs the APK, verifies the signer certificate against `EXPECTED_RELEASE_CERT_SHA256`, generates checksums, uploads the workflow artifact, and publishes the release assets.

Publish only the public SHA-256 certificate fingerprint. Never publish the keystore, private key, alias password, keystore password, or signing password.

### Intentional signing-key rotation

A signing-key change is a security-sensitive release event:

1. Generate and back up the new keystore offline.
2. Read and independently record the new SHA-256 certificate fingerprint.
3. Obtain project-owner approval for the rotation.
4. Publish the old and new fingerprints through a trusted project channel before the new release.
5. Replace the four signing secrets and update `EXPECTED_RELEASE_CERT_SHA256` in one controlled maintenance window.
6. Run the release workflow and verify that the pinned comparison succeeds.
7. Explain upgrade impact in the release notes. Android normally rejects a sideloaded update signed by a different certificate, so users may need to uninstall the previous build unless a supported signing-lineage migration is in place.

Never update the expected fingerprint merely to make a failing release pass. First confirm that the keystore change was intentional.

---

## Security checklist

- [x] Public Client ID only in the APK
- [x] No Client Secret in source, Gradle files, resources, or APK
- [x] `local.properties`, `keystore.properties`, `*.jks`, and `*.keystore` ignored by Git
- [x] GitHub login through official GitHub pages
- [x] Device Flow polling follows GitHub's supplied interval
- [x] Tokens stored with Android Keystore-backed encryption
- [x] Signed release APK verified with `apksigner`
- [x] APK signer compared with `EXPECTED_RELEASE_CERT_SHA256`
- [x] APK checksum and certificate fingerprint published separately
