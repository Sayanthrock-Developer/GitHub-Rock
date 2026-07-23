# GitHub Rock Backend connection

GitHub Rock can connect to the companion Ktor service in [`Sayanthrock-Developer/GitHub-Rock-Backend`](https://github.com/Sayanthrock-Developer/GitHub-Rock-Backend).

The backend is optional by design. Normal repository, issue, pull-request, Actions, release, and download operations continue to call GitHub directly with the user's encrypted OAuth token. The backend currently provides public runtime health/configuration, GitHub OAuth Device Flow start/poll/refresh, and GitHub webhook intake.

## 1. Deploy the backend

Deploy the backend behind HTTPS and configure all production variables listed in its `.env.example`. The OAuth client secret belongs only on that server and must never be copied into this Android repository or an APK.

Verify the deployed service:

```text
GET https://your-backend.example/v1/health
GET https://your-backend.example/v1/config
```

`/v1/config` should report `oauthDeviceProxy=true`. Token refresh through the backend additionally requires `oauthRefreshProxy=true`.

## 2. Connect from the Android app

Open:

**Profile → About → App information → GitHub Rock Backend connection**

Enter the deployed HTTPS base URL and select **Save and test connection**. GitHub Rock checks both `/v1/health` and `/v1/config` before saving the endpoint.

The endpoint can also be bundled at build time:

```properties
GITHUB_ROCK_BACKEND_URL=https://your-backend.example/
```

For GitHub Actions builds, create the repository variable `GITHUB_ROCK_BACKEND_URL`.

## Authentication behavior

1. When a backend endpoint is connected, Device Flow starts and polls through the backend.
2. Expiring OAuth tokens refresh through the backend so the OAuth client secret remains server-side.
3. If the backend is unavailable, GitHub Rock falls back to direct GitHub Device Flow when the public Client ID is present.
4. Access and refresh tokens are stored only in Android Keystore-backed local storage. The backend proxy is stateless and does not persist tokens.

## Security boundaries

- Only HTTPS backend URLs are accepted.
- URLs containing credentials, query parameters, or fragments are rejected.
- GitHub passwords are never requested or handled.
- The OAuth client secret is server-only.
- The backend does not replace GitHub authorization checks.
- Direct GitHub API access remains available during backend maintenance.
