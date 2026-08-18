# Implementation Status

This file separates working alpha functionality from roadmap scope so the application never represents an unfinished action as complete.

## Implemented in the current alpha

- Native Android project, package `com.sayanthrock.githubrock`, API 29–36
- GitHub OAuth App Device Flow request/poll/refresh/logout foundation with explicit scopes
- Android Keystore-backed encrypted token storage and redacted HTTP logging
- Connected, public guest, and isolated demo data modes
- Home, Repositories, Builds, Downloads, Profile, and repository-detail navigation
- App-wide adaptive Material 3 design system with phone bottom navigation, tablet/landscape navigation rail, bounded wide-screen content, and persistent appearance controls
- Pull-to-refresh dashboard, progressive connected-account loading, concurrent account/API/repository requests, and honest loading/empty/error feedback
- Working own-profile repository, follower, and following destinations through the allow-listed GitHub browser launcher
- Native own-profile REST/GraphQL details with yearly contributions, highlights, organizations, pronouns, social links, and ORCID detection; arbitrary profile search and follow permission are intentionally absent
- Repository search filters for language, source/fork or visibility type, sort order, and repository creation
- Full scrollable workflow logs in popup or lazy syntax-highlighted terminal presentation, selectable in Appearance
- Native App & SDK information for package, Android APIs, device, install dates, ABIs, and requested permissions
- Explicit trusted-GitHub image/file downloads with clear format guidance, byte-based transfer details, and resumable progress indicators
- Bulk optional-feature controls with fresh-install and reset defaults set to off
- Searchable All GitHub services hub with 45 allow-listed official web destinations and personalized profile, repository, project, package, and Gist links
- GitHub reads for profile, rate limit, repositories, directories, issues, pull requests, workflows, runs, and releases, including direct public/private repository resolution for deep links
- Five-platform release-asset picker for Android, macOS, Windows, Linux, and iOS with deterministic format and architecture classification
- Installable GitHub Pages web companion plus a Tauri 2 package shell for macOS, Windows, Linux, and signed iOS builds, with an offline application shell, live release assets, and honest scope labels
- Verified workflow dispatch/cancel/rerun repository methods
- Android project detection and safe debug/release APK workflow YAML generation
- Room cache schema, DataStore settings, recoverable WorkManager download worker
- SHA-256 fingerprinting and expected-checksum validation plus APK/package/signature inspection foundation
- Deep-link routes and Android-system installer permission model
- Unit, Compose UI, lint, CI, debug APK, and manually dispatched release workflow configuration

## GitHub Android parity program

The latest parity request is tracked in [issue #235](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues/235).

The target is to provide native Android paths for supported GitHub operations instead of forcing the user to open the website. This includes notifications, issues, pull requests, reviews, repository browsing, workflow/check visibility, releases, and supported merge operations.

The program also adds two specific parity targets:

- **Stacked pull requests:** detect and display stack relationships when GitHub exposes enough information, show stack position/status, navigate parent/child PRs, and expose merge actions only when repository protections and GitHub permissions allow them.
- **Archived agent sessions:** create/resume/restart/rename/search/archive/delete sessions and restore archived sessions when the connected provider supports that operation. Provider-dependent capabilities must never be presented as native GitHub functionality without entitlement/API evidence.

### Platform contract

- **Native Android** — fully implemented inside GitHub Rock on Android 10+.
- **Connected GitHub** — requires the minimum user-approved GitHub authorization and repository permissions.
- **Backend-dependent** — requires a secure service for secrets, schedules, cloud agents, model providers, or long-running jobs.
- **Companion-only** — local git worktrees, unrestricted terminal/shell execution, local LSP/plugin processes, IDE integration, and OS-specific developer tooling.
- **Roadmap** — unfinished functionality must remain visibly disabled or labelled planned.

### Required parity areas

1. Notifications: latest notifications, filtering, read-state where supported, and deep links.
2. Issues: read/create/edit/comment/react/assign/label/milestone/lock/unlock/close/reopen and supported project navigation.
3. Pull requests: browse, review, comments, reactions, reviewers, draft/ready state, review-thread resolution, permitted workflow actions, auto-merge, and merge.
4. Stacked PRs: stack relationship/status/parent-child navigation and protection-aware merge workflow.
5. Agent sessions: lifecycle management plus archive/restore where the provider supports it.
6. Repository browsing: files, directories, branches, commits, releases, tags, workflows, runs, artifacts, and search hand-off.
7. Profile/account: repositories, followers/following, organizations, contributions, and supported profile fields.
8. Releases: platform/architecture asset classification, checksum validation, resumable downloads, and Android package installation through system APIs.
9. Accessibility and reliability: TalkBack, large text, focus/keyboard, reduced motion, contrast, offline, rate-limit, permission-denied, conflict, and recovery states.

### Definition of done

A capability may be marked supported only after its real API integration, minimum permissions, loading/empty/error/offline/recovery states, accessibility validation, unit/UI tests, security checks, and CI evidence are complete.

Do not claim that every website capability is available on Android when GitHub does not expose the required API or when the operation belongs to a desktop/backend execution boundary.

## Copilot workspace parity program

The earlier v1.0.22–v1.0.26 session, MCP, pull-request review, automation, accessibility, files, extensions, and desktop-integration requests are tracked in [issue #163](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues/163).

These capabilities are not treated as complete merely because they appear in another product's changelog. Each item must have the correct platform implementation, permission checks, loading/error/empty states, accessibility, tests, and CI evidence before it is marked supported.

Status rules:

- **Native Android** — works inside GitHub Rock on Android 10+.
- **Connected** — requires GitHub authorization and the minimum repository/account permissions.
- **Backend-dependent** — requires a secure service for secrets, schedules, or long-running jobs.
- **Companion-only** — operating-system integration that belongs in the Tauri desktop companion rather than the Android app.
- **Roadmap** — visible as planned work, never presented as functioning until release evidence exists.

## Copilot CLI, Copilot app, and gh-ost integration program

The broader reference-feature request is tracked in [issue #172](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues/172) and documented in [Reference feature integration](docs/REFERENCE_FEATURE_INTEGRATION.md).

This program is a historical roadmap reference. Its checklists must not be interpreted as proof that every capability is currently shipped in the Android runtime. The current implementation status above and issue #235 are authoritative for the new GitHub Android parity work.

Every milestone must update this file and the in-app feature status only after the implementation, tests, permission checks, accessibility validation, and CI evidence are merged.
