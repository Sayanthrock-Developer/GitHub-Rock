# Security Policy

## Reporting a vulnerability

Please do not open a public issue for a credential leak, authentication bypass, unsafe workflow generation, artifact-verification failure, or installation vulnerability. Use GitHub's private vulnerability reporting for this repository when available, or contact the repository owner privately.

Include the affected commit/version, Android version, reproduction steps, expected behavior, observed behavior, and whether a token or signing credential may have been exposed. Never include a live token, private key, client secret, keystore, or password in the report.

## Supported versions

The project is currently an alpha. Security fixes are applied to the latest `main` branch and the most recent release only.

## Credential model

GitHub Rock embeds only a public GitHub App client ID. User tokens are encrypted with an Android Keystore-backed key. Android signing material belongs in GitHub repository/environment secrets and is used only on ephemeral Actions runners.

