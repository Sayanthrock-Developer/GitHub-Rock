# Implementation Status

This file separates working alpha functionality from roadmap scope so the application never represents an unfinished action as complete.

## Implemented in 0.1.0 alpha

- Native Android project, package `com.sayanthrock.githubrock`, API 29–36
- GitHub App Device Flow request/poll/refresh/logout foundation
- Android Keystore-backed encrypted token storage and redacted HTTP logging
- Connected, public guest, and isolated demo data modes
- Home, Repositories, Builds, Downloads, Profile, and repository-detail navigation
- GitHub reads for profile, rate limit, repositories, directories, issues, pull requests, workflows, runs, and releases
- Verified workflow dispatch/cancel/rerun repository methods
- Android project detection and safe APK/AAB workflow YAML generation
- Room cache schema, DataStore settings, recoverable WorkManager download worker
- SHA-256 validation and APK/package/signature inspection foundation
- Deep-link routes and Android-system installer permission model
- Unit, Compose UI, lint, CI, debug APK, and manually dispatched release workflow configuration

## Next implementation milestones

1. Code viewer/editor and reviewed branch/commit/pull-request mutation flow.
2. Complete issue, pull-request review, checks, and confirmed merge UI.
3. Build wizard, workflow preview/branch commit/PR, run logs, and artifact picker.
4. Download queue controls, APK inspection presentation, and system installer launch.
5. Draft release editor, asset upload, release notes, and destructive confirmations.
6. Paging-backed large lists, GraphQL batching, biometric lock, adaptive panes, and accessibility/device screenshot validation.

