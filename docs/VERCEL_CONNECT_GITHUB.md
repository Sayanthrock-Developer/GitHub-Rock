# Vercel Connect — GitHub connector setup

This guide fixes the configuration screen shown under **Vercel → Connect → Add Connection → GitHub → Your own credentials**.

## Important distinction

GitHub Rock uses a **GitHub OAuth App with Device Flow** for Android sign-in. That OAuth App does not provide the App ID, private key, or webhook configuration required by Vercel Connect.

Vercel Connect requires a separate **GitHub App**. Do not replace the Android OAuth App and do not put any secret or private key in this repository, the APK, Gradle files, Actions variables, source code, screenshots, issues, or pull requests.

## 1. Open or create the GitHub App

Open:

**GitHub → Settings → Developer settings → GitHub Apps**

Use the existing GitHub App for GitHub Rock when it is correctly owned and configured, or create a dedicated app for the Vercel connector.

Recommended values:

| GitHub App field | Value |
| --- | --- |
| GitHub App name | `Sayanth Rock Mobile` or another unique name |
| Homepage URL | `https://github.com/Sayanthrock-Developer/GitHub-Rock` |
| Callback URL | `https://connect.vercel.com/callback` |
| Expire user authorization tokens | Enabled when supported by the integration |
| Request user authorization during installation | Enabled only when Vercel requires user-scoped access |
| Webhook | Active |

The **App slug** is the final segment of the GitHub App URL. Example:

```text
https://github.com/apps/sayanth-rock-mobile
                         └── sayanth-rock-mobile
```

## 2. Configure permissions conservatively

Grant only the permissions needed by the connector. Start with read-only access and add write permissions only for features that actually need them.

Typical repository permissions may include:

- Metadata: Read-only
- Contents: Read-only, or Read and write only when file changes are required
- Pull requests: Read-only, or Read and write only when PR creation or updates are required
- Issues: Read-only, or Read and write only when issue actions are required
- Actions: Read-only, or Read and write only when workflow dispatch or reruns are required
- Checks: Read-only when check results are needed
- Deployments: Read-only when deployment status is needed

Install the GitHub App only on the required account or organization and select only the repositories that Vercel Connect must access.

## 3. Create the required credentials

From the GitHub App **General** settings page:

1. Copy the numeric **App ID**.
2. Copy the **Client ID**.
3. Generate a new **Client secret** and copy it immediately.
4. Generate a **Private key** and download the `.pem` file.
5. Create a strong random **Webhook secret** and store it in a password manager.
6. Save the same webhook secret in the GitHub App webhook settings and in Vercel Connect.

Never commit any of these values:

```text
Client Secret
Private Key
Webhook Secret
```

## 4. Complete the Vercel form

Use the following mapping:

| Vercel field | Required value |
| --- | --- |
| Connector Name | A unique label such as `github-rock` |
| UID | Keep the generated `github/<name>` value unless Vercel requires another unique value |
| App Slug | GitHub App URL slug, for example `sayanth-rock-mobile` |
| App ID | Numeric GitHub App ID |
| Client ID | GitHub App Client ID |
| Client Secret | Newly generated GitHub App client secret |
| Private Key | Full PEM contents, including the BEGIN and END lines |
| Webhook Secret | Same secret configured in the GitHub App |
| Redirect URI | `https://connect.vercel.com/callback` |

The private key must be pasted exactly in PEM format:

```text
-----BEGIN RSA PRIVATE KEY-----
...
-----END RSA PRIVATE KEY-----
```

or, depending on the generated key format:

```text
-----BEGIN PRIVATE KEY-----
...
-----END PRIVATE KEY-----
```

Do not remove line breaks.

## 5. Create and test the connector

1. Press **Create GitHub Connector** in Vercel.
2. Complete the GitHub authorization or installation step.
3. Install the GitHub App on `Sayanthrock-Developer` only when that is the intended account.
4. Select `GitHub-Rock` or the minimum required repository set.
5. Confirm that Vercel can read the selected repository.
6. Test one non-destructive action before enabling write access.

## Common failures

### App slug is rejected

Use only the slug from the GitHub App URL. Do not use the app display name, repository name, organization name, OAuth App name, or Client ID.

### App ID or Client ID is rejected

Copy both values from **GitHub App → General**. Values from an OAuth App are not interchangeable.

### Redirect URI mismatch

Add this exact URL to the GitHub App callback configuration:

```text
https://connect.vercel.com/callback
```

The scheme, host, path, and trailing slash must match the value shown by Vercel.

### Private key is invalid

Generate a fresh key from the GitHub App settings and paste the complete PEM contents with the header, footer, and line breaks intact.

### Webhook signature verification fails

Set the same webhook secret on both GitHub and Vercel. Remove accidental spaces or line breaks around the value.

### Installation succeeds but no repository appears

Edit the GitHub App installation and grant access to the intended organization and repository. Also confirm that the GitHub App permissions include repository metadata and the required read scopes.

### The Android login stops working

The Vercel connector and Android Device Flow are separate. Keep the existing Android OAuth App Client ID configuration unchanged.

## Security checklist

- [ ] GitHub App used for Vercel Connect
- [ ] Android OAuth App kept separate
- [ ] Exact Vercel callback URL configured
- [ ] App slug copied from the GitHub App URL
- [ ] Private key pasted with complete PEM formatting
- [ ] Matching webhook secret configured on both sides
- [ ] GitHub App installed only on required repositories
- [ ] Minimum permissions granted
- [ ] No secret committed to GitHub or bundled into the APK
- [ ] Old credentials revoked after any accidental exposure

## Credential exposure response

When a Client Secret, Private Key, or Webhook Secret is exposed in a screenshot, issue, commit, log, or chat:

1. Revoke or delete the exposed credential immediately.
2. Generate a replacement.
3. Update Vercel Connect with the replacement.
4. Verify the old credential no longer works.
5. Review GitHub App installations, webhook deliveries, audit logs, and repository activity.

Do not reuse an exposed credential.