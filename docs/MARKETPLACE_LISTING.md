# GitHub Marketplace Listing

Use these values for the GitHub Marketplace listing named **Sayanth Rock Mobile OAuth**.

## 1 — Naming and links

| Field | Value |
| --- | --- |
| Listing name | `Sayanth Rock Mobile OAuth` |
| Very short description | `Secure Android control centre for GitHub repositories, Actions, pull requests, releases, and downloads.` |
| Primary category | `Developer tools` when available; otherwise `Utilities` |
| Secondary category | `Continuous integration` when available; otherwise `Project management` |
| Supported languages | `English` |
| Customer support URL | `https://github.com/Sayanthrock-Developer/GitHub-Rock/issues/new/choose` |
| Installation URL | `https://github.com/Sayanthrock-Developer/GitHub-Rock/releases` |
| Company URL | `https://github.com/Sayanthrock-Developer` |
| Status URL | Leave blank until a real public status page exists |
| Documentation URL | `https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/docs/GITHUB_AUTH_AND_SIGNING.md` |

Do not use **Agent apps** as the primary category. GitHub Rock is a developer tool, not an autonomous agent.

After entering the values, select **Save naming and links**.

## 2 — Logo and feature card

Upload the repository assets from `docs/marketplace/`:

| Field | Value |
| --- | --- |
| Logo | `docs/marketplace/github-rock-marketplace-logo.png` |
| Background image | `docs/marketplace/github-rock-marketplace-feature-card.png` |
| Badge background color | `0D1117` |
| Text color | `Light text` |

The feature card is exactly `965 × 482 px`. The logo is a square `1024 × 1024 px` PNG.

After uploading both files, select **Save logo and feature card**.

## 3 — Plans and pricing

Create one plan by selecting **New draft plan**.

| Field | Value |
| --- | --- |
| Plan name | `Free` |
| Plan type | `Free` |
| Short description | `Full GitHub Rock access with secure OAuth Device Flow and no subscription fee.` |
| Monthly price | `0` |
| Annual price | `0` |
| Trial | No trial needed |
| Units or seats | Not applicable |

Do not create a paid plan unless a real paid service, billing process, support policy, and Marketplace purchase webhook are introduced.

Save the plan and make it visible in the draft listing.

## 4 — Security and compliance

### EU Digital Services Act trader status

The repository cannot make this legal declaration on the owner's behalf. For a personal, free, open-source project with no commercial Marketplace activity in the EU, select:

`I do not operate as a trader under EU regulations`

Select that option only when it accurately describes the owner and project. Obtain legal advice when uncertain.

### Security fields

| Field | Value |
| --- | --- |
| Privacy Policy URL | `https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/PRIVACY.md` |
| Terms of Service URL | `https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/TERMS.md` |
| Third-party services required | `GitHub OAuth and GitHub APIs; Android browser/Custom Tabs; Android Package Installer and system sharing/file interfaces; optional Termux integration when explicitly enabled by the user.` |
| Repository visibility | `Public` |

### Transparency disclosures

Paste this Markdown into the disclosure field:

```markdown
## Security and safety overview

GitHub Rock is an open-source Android developer tool that connects to GitHub through OAuth Device Flow. Authorization occurs only on GitHub's official website. The app never asks users to enter a GitHub password and never embeds an OAuth Client Secret.

### Authentication and access controls
- The app requests only the documented OAuth scopes needed for repository, workflow, profile, organization, email, and notification features.
- Users review and approve access on GitHub and may revoke authorization at any time from GitHub account settings.
- OAuth access and refresh tokens are stored locally using Android Keystore-backed encrypted storage.
- Stored sessions are bound to the configured OAuth Client ID; incompatible legacy sessions are cleared.

### Data handling
- GitHub account and repository data is requested from GitHub only to provide user-selected features.
- The project owner does not receive OAuth tokens during normal app operation.
- App backup is disabled for sensitive data, cleartext traffic is disabled, authorization and cookie headers are redacted, and release builds disable HTTP logging.
- Logging out clears the local OAuth session. Users may also revoke authorization, clear app storage, or uninstall the app.

### User-controlled operations
- Repository changes, workflow actions, downloads, sharing, and APK installation are initiated by the user.
- APK installation is delegated to Android's official Package Installer. GitHub Rock does not bypass Android signature checks, Play Protect, repository protections, or organization policy.
- Optional Termux integration runs only when explicitly selected by the user.

### Incident reporting and limitations
- Security issues must be reported privately using the repository's SECURITY.md instructions; live credentials must never be posted publicly.
- GitHub Rock is provided under the Apache License 2.0 without an uptime or service-level guarantee.
- GitHub services and data processing remain subject to GitHub's own terms, privacy statement, API rules, and organization policies.
```

Select **Save listing details**.

## 5 — Webhook

The current Android OAuth Device Flow implementation has no backend webhook receiver.

- Leave **Payload URL** blank.
- Do not enter a fake endpoint.
- Leave **Secret** blank.
- Turn **Active** off.
- Do not select **Create webhook**.

A real HTTPS webhook endpoint, secret verification, event storage policy, retry handling, and incident monitoring are required before enabling Marketplace purchase events. Paid plans must not be offered without that backend.

## Related listing pages

| Marketplace requirement | URL |
| --- | --- |
| Privacy policy | `https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/PRIVACY.md` |
| Terms of service | `https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/TERMS.md` |
| Support | `https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/SUPPORT.md` |
| Security policy | `https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/SECURITY.md` |
| OAuth documentation | `https://github.com/Sayanthrock-Developer/GitHub-Rock/blob/main/docs/GITHUB_AUTH_AND_SIGNING.md` |

## Notes

- GitHub Rock uses OAuth Device Flow.
- The public OAuth Client ID may be included in the Android app.
- Never upload or publish a Client Secret, access token, refresh token, private key, keystore, or password.
- Use a free plan unless paid services are actually provided.
