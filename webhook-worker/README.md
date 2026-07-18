# GitHub Rock Marketplace Webhook

This Cloudflare Worker provides the HTTPS endpoint required by the GitHub Marketplace listing.

## Endpoints

- `GET /health` — public health check
- `POST /github/marketplace/webhook` — signed GitHub Marketplace webhook receiver

The worker:

- Requires `X-Hub-Signature-256` HMAC verification.
- Accepts JSON and GitHub form-encoded webhook payloads.
- Handles `ping` and `marketplace_purchase` events.
- Rejects missing, malformed, oversized, or incorrectly signed requests.
- Does not log webhook payloads, OAuth tokens, email addresses, or account details.
- Does not store Marketplace event data because the current listing has one free plan.

## 1 — Create Cloudflare credentials

Create or use a Cloudflare account, then obtain:

- A Cloudflare API token allowed to edit Workers.
- Your Cloudflare account ID.

Do not commit either value.

## 2 — Create one webhook secret

Generate a random secret in a trusted terminal or password manager:

```bash
openssl rand -hex 32
```

Keep the generated value private. The exact same value must be entered in both places:

1. GitHub Actions secret `MARKETPLACE_WEBHOOK_SECRET`.
2. GitHub Marketplace listing → Webhook → Secret.

Do not post the secret in an issue, pull request, commit, screenshot, chat, log, or release.

## 3 — Add GitHub Actions secrets

Open:

**Repository → Settings → Secrets and variables → Actions → New repository secret**

Create:

```text
CLOUDFLARE_API_TOKEN
CLOUDFLARE_ACCOUNT_ID
MARKETPLACE_WEBHOOK_SECRET
```

## 4 — Deploy

Open:

**Actions → Deploy Marketplace Webhook → Run workflow**

The workflow tests the signature verifier and deploys the Worker. Copy the `workers.dev` URL from the deploy output.

Your Marketplace Payload URL is:

```text
https://github-rock-marketplace-webhook.<your-workers-subdomain>.workers.dev/github/marketplace/webhook
```

Your health URL is:

```text
https://github-rock-marketplace-webhook.<your-workers-subdomain>.workers.dev/health
```

Opening the health URL should return:

```json
{"status":"ok","service":"github-rock-marketplace-webhook"}
```

## 5 — Configure GitHub Marketplace

Use:

| Field | Value |
| --- | --- |
| Payload URL | The deployed Worker URL ending in `/github/marketplace/webhook` |
| Content type | `application/json` |
| Secret | The same private value stored as `MARKETPLACE_WEBHOOK_SECRET` |
| Active | Enabled |

Then select **Create webhook**.

Never use a GitHub Pages URL or an invented domain. The URL must be the real deployed HTTPS Worker endpoint.

## Local tests

Node.js 20 or newer is required:

```bash
cd webhook-worker
npm test
```

## Secret rotation

To rotate the webhook secret safely:

1. Generate a new random value.
2. Update `MARKETPLACE_WEBHOOK_SECRET` in GitHub Actions.
3. Run **Deploy Marketplace Webhook** again.
4. Immediately replace the Secret in the Marketplace webhook settings with the same new value.
5. Send or redeliver a test event and confirm a successful response.

During the short interval between steps 3 and 4, webhook deliveries will fail signature verification. Perform rotation in one maintenance window.
