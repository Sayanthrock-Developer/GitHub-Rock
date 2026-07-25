# GitHub App registration and backend security

GitHub Rock keeps its Android OAuth Device Flow separate from its backend GitHub App.

- The **Android OAuth App** signs users in and stores only user-approved tokens on the device.
- The **GitHub App** is installed on selected repositories and is used only by a trusted backend.
- The GitHub App private key, webhook secret, client secret, and installation access tokens must never be committed, logged, returned to the Android client, or embedded in the APK.

## Included files

- Manifest: [`.github/github-app-manifest.json`](../.github/github-app-manifest.json)
- Backend environment template: [`docs/github-app.env.example`](github-app.env.example)
- Existing Vercel connector notes: [`docs/VERCEL_CONNECT_GITHUB.md`](VERCEL_CONNECT_GITHUB.md)

## Least-privilege permissions

The manifest grants read-only repository permissions:

| Permission | Access | Purpose |
| --- | --- | --- |
| Actions | Read | Workflow runs, jobs, logs, and artifacts metadata |
| Checks | Read | Check runs and check suites |
| Contents | Read | Releases, release assets, tags, and repository content required by read endpoints |
| Issues | Read | Issues and issue metadata |
| Metadata | Read | Basic repository metadata required by GitHub Apps |
| Pull requests | Read | Pull requests, reviews, and changed-file metadata |

No write permission is requested. In particular, the manifest does not grant workflow dispatch, rerun, merge, issue-edit, pull-request-edit, contents-write, administration, secrets, variables, or organization-management access.

## 1. Prepare a trusted backend

Use a server, serverless function, or worker that can safely store secrets. The backend must provide HTTPS routes for:

- GitHub App manifest callback
- GitHub webhook delivery
- Any narrow API endpoints consumed by GitHub Rock

The committed manifest intentionally uses `https://replace-me.invalid/...` routes. Replace both URLs with real HTTPS backend routes before starting registration.

Do not use GitHub Pages as the manifest callback or webhook receiver. Static pages cannot securely exchange the manifest code, hold the generated private key, validate webhook signatures, or mint installation access tokens.

## 2. Register from the manifest

GitHub App manifest registration requires a backend-assisted flow:

1. Load `.github/github-app-manifest.json` on the trusted backend.
2. Replace the manifest `redirect_url` and webhook URL with the deployed backend URLs.
3. Generate an unpredictable CSRF `state` value and store it in a secure, short-lived server session.
4. POST the manifest to the correct GitHub App registration URL for the intended owner.
5. After GitHub redirects to the manifest callback, verify `state` before processing `code`.
6. Exchange the temporary code through GitHub's App Manifest conversion endpoint.
7. Immediately place the returned App ID, Client ID, Client Secret, webhook secret, and private key into a secret manager.
8. Do not print, persist in normal application logs, send to analytics, or return the conversion response to the browser or Android app.

The manifest conversion code is temporary and should be exchanged once. Treat the conversion response as highly sensitive because it contains newly generated credentials.

## 3. Configure backend environment values

Copy the variable names from `docs/github-app.env.example` into the deployment platform's encrypted environment or secret store.

### Private key format

The recommended template variable is:

```text
GITHUB_APP_PRIVATE_KEY_BASE64=
```

Base64-encode the complete PEM file, including its BEGIN and END lines, and decode it only in backend memory. This avoids accidental newline damage in environment dashboards.

Never commit any of these values:

```text
GITHUB_APP_CLIENT_SECRET
GITHUB_APP_PRIVATE_KEY_BASE64
GITHUB_APP_WEBHOOK_SECRET
installation access tokens
user access tokens
```

App ID, Client ID, and app slug are identifiers rather than authentication secrets, but keeping all GitHub App configuration backend-side reduces accidental coupling with the Android build.

## 4. Install on the minimum repository set

For the organization-owned app:

1. Install it only on `Sayanthrock-Developer` when that is the intended owner.
2. Select **Only select repositories**.
3. Grant access only to `GitHub-Rock` unless another repository has a documented requirement.
4. Review the requested permissions before accepting the installation.
5. Record the installation ID in the backend secret store when the backend requires an explicit installation allow-list.

Do not enable access to all present and future repositories by default.

## 5. Mint installation tokens only on the backend

A trusted backend may:

1. Create a short-lived GitHub App JWT signed with the private key.
2. Resolve the approved installation.
3. Request an installation access token limited to the selected repositories and permissions.
4. Use that token for the required GitHub API call.
5. Discard the token when the request or short cache window ends.

The backend must never return the JWT, private key, client secret, webhook secret, or installation token to Android. Return only the minimum application data needed by the screen.

Apply both controls even though GitHub already scopes the installation:

- owner/repository allow-list
- endpoint/action allow-list

Reject arbitrary owner, repository, URL, GraphQL document, REST path, or HTTP method values supplied by the mobile client.

## 6. Verify every webhook

For each webhook request:

1. Read the raw request bytes before JSON parsing.
2. Calculate HMAC-SHA-256 with `GITHUB_APP_WEBHOOK_SECRET`.
3. Compare it in constant time with `X-Hub-Signature-256`.
4. Require a known `X-GitHub-Event` value.
5. Track `X-GitHub-Delivery` temporarily to reject duplicate or replayed deliveries.
6. Validate the installation, repository owner, and repository name against the allow-list.
7. Return a successful response quickly and perform longer work through a trusted queue when needed.

Do not accept unsigned webhook payloads, signature values calculated after body mutation, or webhook secrets supplied by the client.

## 7. Android boundary

The Android application must not contain GitHub App credentials in:

- source code or resources
- `BuildConfig`
- Gradle properties
- `local.properties`
- assets or raw resources
- native libraries
- DataStore, Room, or SharedPreferences
- Android Keystore entries shipped or provisioned as app-owned shared secrets
- release artifacts, logs, screenshots, crash reports, or analytics

Android Keystore can protect a specific user's local token from casual extraction. It cannot make a shared GitHub App private key safe inside a publicly distributed APK because the application must eventually be able to use that key.

## 8. Rotation and incident response

When a private key, client secret, webhook secret, or installation token may have been exposed:

1. Revoke or delete it immediately in GitHub.
2. Generate a replacement.
3. Update the backend secret manager.
4. Redeploy and verify the replacement.
5. Confirm the old credential no longer works.
6. Review installations, webhook deliveries, audit logs, Actions logs, release artifacts, issues, pull requests, and repository history.
7. Purge exposed values from logs and caches where supported.

Deleting a secret from the latest commit does not remove it from Git history. Rotate first; history cleanup is secondary.

## 9. Validation checklist

- [ ] Placeholder `.invalid` URLs replaced before registration
- [ ] App is private unless public installation is intentionally required
- [ ] Only read permissions shown in this guide are enabled
- [ ] App installed only on required repositories
- [ ] Private key stored in a backend secret manager
- [ ] Client secret stored only on the backend
- [ ] Webhook secret stored only on the backend
- [ ] Webhook signatures verified against raw request bytes
- [ ] Installation tokens never returned to Android
- [ ] Backend owner/repository and endpoint allow-lists enabled
- [ ] Secrets redacted from logs, crashes, analytics, and CI output
- [ ] Credential rotation procedure tested

## Permission changes

Any future write capability must be introduced through a separate reviewed change that documents:

- exact GitHub endpoints requiring write access
- user-visible action and confirmation flow
- repository and installation scope
- backend authorization checks
- tests and audit logging
- rollback and credential-rotation impact

Do not broaden the shared read-only manifest merely to avoid creating a purpose-specific GitHub App or permission profile.
