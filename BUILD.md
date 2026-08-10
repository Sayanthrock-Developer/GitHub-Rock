# Build GitHub Rock

## Requirements

- Android Studio with the Android SDK and build-tools installed
- JDK 17
- Android SDK 36
- A GitHub OAuth App client ID configured through `local.properties` or the environment when a non-default client is required

## Local setup

1. Copy `local.properties.example` to `local.properties`.
2. Set `sdk.dir` to the local Android SDK path.
3. Optionally set `GITHUB_CLIENT_ID` and `GITHUB_ROCK_BACKEND_URL` for local development.
4. Do not commit `local.properties`, signing files, tokens, or secrets.

## Verification commands

Use the committed wrapper so local and CI builds resolve the same Gradle distribution:

```bash
./gradlew --version
./gradlew lint
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew assembleDebugAndroidTest
```

The debug APK is written under `app/build/outputs/apk/debug/`. Release builds are minified and require signing configuration for a signed artifact.

## Signed release build

The release workflow reads the keystore from protected GitHub Actions secrets. It validates the reviewed signing certificate fingerprint, builds the release APK, generates a SHA-256 checksum, and publishes verified assets. Never put a keystore, password, private key, or client secret in the repository.

For local signing, provide the same values through environment variables used by `app/build.gradle.kts`:

- `GITHUB_ROCK_KEYSTORE_PATH`
- `GITHUB_ROCK_KEYSTORE_PASSWORD`
- `GITHUB_ROCK_KEY_ALIAS`
- `GITHUB_ROCK_KEY_PASSWORD`

## Troubleshooting

- If Gradle cannot find the SDK, fix `sdk.dir` in `local.properties`.
- If authentication configuration is missing, use guest/demo mode or provide a public client ID; never add a client secret.
- If a release certificate changes intentionally, rotate the protected keystore and reviewed fingerprint together.
