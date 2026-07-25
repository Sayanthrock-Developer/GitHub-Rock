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

## Copilot workspace parity program

The v1.0.22–v1.0.26 session, MCP, pull-request review, automation, accessibility, files, extensions, and desktop-integration requests are tracked in [issue #163](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues/163).

These capabilities are not treated as complete merely because they appear in another product's changelog. Each item must have the correct platform implementation, permission checks, loading/error/empty states, accessibility, tests, and CI evidence before it is marked supported.

Status rules:

- **Native Android** — works inside GitHub Rock on Android 10+.
- **Connected** — requires GitHub authorization and the minimum repository/account permissions.
- **Backend-dependent** — requires a secure service for secrets, schedules, or long-running jobs.
- **Companion-only** — operating-system integration that belongs in the Tauri desktop companion rather than the Android app.
- **Roadmap** — visible as planned work, never presented as functioning until release evidence exists.

The active parity phases are:

1. Issue/PR handoff, commit navigation, review progress, incremental diffs, stale-data recovery, and permission-aware controls.
2. Trusted project MCP settings with OAuth, refresh, enable/disable, and secure credential handling.
3. Local and cloud automations with quarter-hour scheduling, live run states, filters, and accessible timestamps.
4. Session recovery and tool-approval commands, including `/allow-all-tools` and `/yolo` state controls.
5. Files and extensions panels, trusted URL installation, workspace persistence, and storage warnings.
6. Complete TalkBack, keyboard, large-text, contrast, focus, and announcement coverage.
7. Windows, macOS, and Linux companion integration for clipboard, browser, tray, PATH, WSL, VS Code, and terminal attachment behavior.

## Copilot CLI, Copilot app, and gh-ost integration program

The broader reference-feature request is tracked in [issue #172](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues/172) and documented in [Reference feature integration](docs/REFERENCE_FEATURE_INTEGRATION.md).

This program is currently **Roadmap**. Creating the issue and feature matrix does not mark the runtime capabilities as implemented.

The program separates features by their real execution boundary:

- **Native Android / Connected GitHub** — My Work, GitHub references, review state, workflow/check visibility, permission-aware actions, agent-session client UI, automation monitoring, and approved remote-job controls.
- **Backend-dependent** — cloud agents, model-provider access, schedules, remote MCP, installation credentials, audit services, and long-running operations.
- **Companion-only** — local worktrees, terminal/shell execution, sandbox processes, IDE focus, LSP servers, local plugins, and local `gh-ost` execution.
- **Remote operations** — MySQL topology access and migration execution remain on an approved server or companion. Android may use only a typed, authenticated, allow-listed control API.

No GitHub Copilot entitlement, model access, unrestricted tool execution, desktop worktree, or MySQL migration capability may be advertised as working merely because a UI entry or documentation checklist exists.

## Next implementation milestones

1. Build issue #172 Phase A: native My Work foundation, GitHub reference hand-off, cached-first detail, and permission-aware actions.
2. Add the agent-session client contract with honest provider/backend availability and reversible tool-approval state.
3. Implement desktop worktrees, terminal, sandbox, IDE hand-off, and local MCP/LSP/plugin processes in the companion rather than Android.
4. Add extensibility canvases, automation run history, and Agent Merge readiness without bypassing repository protections.
5. Design the typed `gh-ost` backend contract and validate it only against disposable MySQL test environments before exposing migration controls.
6. Richer language grammars plus PR diff and conflict presentation.
7. Dynamic `workflow_dispatch` input forms and workflow failure annotations.
8. Storage Access Framework download locations, mirror selection, and trusted checksum-file matching.
9. Biometric lock controls, foldable list-detail panes, and Paging-backed large lists.
10. Complete TalkBack, large-font, keyboard, contrast, and physical-device screenshot validation.
11. Extract portable domain and network layers before moving Android-only control-centre features into the packaged desktop and iOS companion.
12. Continue moving high-value web-hub destinations into native screens when GitHub permissions and APIs support them safely.

Every milestone must update this file and the in-app feature status only after the implementation, tests, permission checks, accessibility validation, and CI evidence are merged.