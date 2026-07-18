# 🔐 GitHub Rock authentication and APK signing

This guide matches the production architecture used by GitHub Rock.

GitHub Rock is a native Android application that uses **GitHub OAuth App Device Flow**. Device Flow needs only a public Client ID. GitHub Rock never embeds a Client Secret, private key, access token, signing key, or password.

## TL;DR

1. Create a GitHub OAuth App.
2. Enable Device Flow.
3. Use the public Client ID `Ov23lim8WhLjeUMqvuMj`.
4. Put it in `local.properties` for local builds, or override it with the `PUBLIC_GITHUB_CLIENT_ID` Actions variable.
5. Sync and run the project.
6. Keep the Client Secret out of the Android app and repository.
7. Before publishing a release, set `EXPECTED_RELEASE_CERT_SHA256` from the independently controlled release keystore.

---

## 🔑 GitHub OAuth configuration

<details>
<summary><strong>Show full setup guide</strong></summary>

### 1 — Create the OAuth App

Open:

**GitHub → Settings → Developer settings → OAuth Apps → New OAuth App**

Use:

| Field | Value |
| --- | --- |
| Application name | `Sayanth Rock Mobile Oauth` |
| Homepage URL | `https://github.com/Sayanthrock-Developer/GitHub-Rock` |
| Application description | `GitHub developer control centre for Android` |
| Authorization callback URL | `githubrock://oauth/callback` |

GitHub requires a callback value when registering an OAuth App. The current Android login uses Device Flow, so this callback is not used for token exchange.

### 2 — Enable Device Flow

Inside the OAuth App settings:

1. Check **Enable Device Flow**.
2. Press **Update application**.
3. Copy the public Client ID.

Connected public Client ID:

```text
Ov23lim8WhLjeUMqvuMj
```

> Do not generate or add a Client Secret for the Android Device Flow implementation. A secret embedded in an APK can be extracted.

### 3 — Local configuration

Copy `local.properties.example` to `local.properties` in the repository root:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
GITHUB_CLIENT_ID=Ov23lim8WhLjeUMqvuMj
```

`local.properties` is ignored by Git.

The app module exposes the value through:

```kotlin
BuildConfig.GITHUB_CLIENT_ID
```

The same Client ID is bundled as the official fallback for CI and release builds. Forks can override it under:

**Repository → Settings → Secrets and variables → Actions → Variables**

```text
Name: PUBLIC_GITHUB_CLIENT_ID
Value: YOUR_PUBLIC_OAUTH_APP_CLIENT_ID
```

A Client ID is public. Client Secrets and tokens are not.

### 4 — Requested OAuth scopes

GitHub Rock requests these scopes during Device Flow authorization:

| Scope | Purpose |
| --- | --- |
| `repo` | Public and private repository operations, issues, pull requests, releases, and Actions data |
| `workflow` | Create or update GitHub Actions workflow files |
| `read:user` | Read the connected account profile |
| `user:email` | Read account email addresses where required |
| `read:org` | Read organization membership needed for repository access and reviewer resolution |
| `notifications` | Read and manage GitHub notifications used by the app |

Users see the requested access on GitHub's official authorization page before approving it.

### 5 — How sign-in works

1. GitHub Rock requests a device code using the OAuth App Client ID and scopes.
2. The app displays the verification code.
3. GitHub opens in an Android Custom Tab.
4. The user approves GitHub Rock on GitHub's official page.
5. GitHub Rock polls only at GitHub's supplied interval.
6. The returned token is stored with Android Keystore-backed encryption.

No embedded WebView password form is used. GitHub Rock never asks for or stores a GitHub password.

### 6 — Build and test

Use Android Studio with JDK 17 and Android SDK 36:

```bash
gradle wrapper --gradle-version 8.13
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleRelease
```

The debug APK is generated under:

```text
app/build/outputs/apk/debug/
```

</details>

---

## 🔏 APK signing certificate

Official releases are signed in GitHub Actions. The release workflow verifies the APK with Android `apksigner`, compares the detected signer certificate with a separately configured trusted fingerprint, and only then publishes release assets.

### Pin the trusted release certificate

Read the fingerprint from the intended release keystore before the first release:

```bash
keytool -list -v \
  -keystore release.jks \
  -alias your_key_alias
```

Create this GitHub Actions repository variable:

```text
Name: EXPECTED_RELEASE_CERT_SHA256
Value: YOUR_TRUSTED_64_HEX_SHA256_FINGERPRINT
```

Colons and letter case are accepted. The workflow normalizes the value and fails closed when it is missing, malformed, or does not match the APK signer.

### Required signing secrets

Create these under **Repository → Settings → Secrets and variables → Actions → Secrets**:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

The workflow decodes the keystore only inside the temporary Actions runner, validates it, signs the APK, verifies the signer against `EXPECTED_RELEASE_CERT_SHA256`, and publishes the release assets.

### Published release assets

| Release asset | Meaning |
| --- | --- |
| `GitHub-Rock-<version>.apk.sha256` | SHA-256 checksum of the APK file |
| `GitHub-Rock-<version>.apk.certificate.sha256` | SHA-256 fingerprint detected from the APK signing certificate |

The certificate file is a convenience copy, not the root of trust. Compare it with a fingerprint previously published through an independently trusted project channel.

### Read the signer from an APK

```bash
apksigner verify --verbose --print-certs GitHub-Rock-<version>.apk
```

Look for:

```text
Signer #1 certificate SHA-256 digest: ...
```

### Intentional key rotation

1. Generate and back up the new keystore offline.
2. Record its SHA-256 certificate fingerprint independently.
3. Obtain project-owner approval.
4. Publish the old and new fingerprints through a trusted channel.
5. Replace the signing secrets and update `EXPECTED_RELEASE_CERT_SHA256` in one controlled maintenance window.
6. Verify the new release and explain upgrade impact in its release notes.

Never update the expected fingerprint only to make a failing release pass.

---

## Security checklist

- [x] Public OAuth Client ID only in the APK
- [x] No Client Secret in source, Gradle files, resources, or APK
- [x] Explicit OAuth scopes requested during Device Flow
- [x] `local.properties`, `keystore.properties`, `*.jks`, and `*.keystore` ignored by Git
- [x] GitHub authorization through official GitHub pages
- [x] Device Flow polling follows GitHub's supplied interval
- [x] Tokens stored with Android Keystore-backed encryption
- [x] Signed release APK verified with `apksigner`
- [x] APK signer compared with `EXPECTED_RELEASE_CERT_SHA256`
- [x] APK checksum and certificate fingerprint published separately
