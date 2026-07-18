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
- Working profile repository, follower, and following destinations through the allow-listed GitHub browser launcher
- Native mobile profile lookup with REST/GraphQL details, follow/unfollow, yearly contributions, highlights, organizations, pronouns, social links, and ORCID detection
- Repository search filters for language, source/fork or visibility type, sort order, and repository creation
- Full scrollable workflow logs in popup or lazy syntax-highlighted terminal presentation, selectable in Appearance
- Native App & SDK information for package, Android APIs, device, install dates, ABIs, and requested permissions
- Searchable All GitHub services hub with 45 allow-listed official web destinations and personalized profile, repository, project, package, and Gist links
- GitHub reads for profile, rate limit, repositories, directories, issues, pull requests, workflows, runs, and releases, including direct public/private repository resolution for deep links
- Five-platform release-asset picker for Android, Windows, Linux, iOS, and macOS with deterministic format and architecture classification
- Verified workflow dispatch/cancel/rerun repository methods
- Android project detection and safe APK/AAB workflow YAML generation
- Room cache schema, DataStore settings, recoverable WorkManager download worker
- SHA-256 fingerprinting and expected-checksum validation plus APK/package/signature inspection foundation
- Deep-link routes and Android-system installer permission model
- Unit, Compose UI, lint, CI, debug APK, and manually dispatched release workflow configuration

## Next implementation milestones

1. Richer language grammars plus PR diff and conflict presentation.
2. Dynamic `workflow_dispatch` input forms and workflow failure annotations.
3. Storage Access Framework download locations, mirror selection, and trusted checksum-file matching.
4. Biometric lock controls, foldable list-detail panes, and Paging-backed large lists.
5. Complete TalkBack, large-font, keyboard, contrast, and physical-device screenshot validation.
6. Continue moving high-value web-hub destinations into native screens when GitHub permissions and APIs support them safely.
