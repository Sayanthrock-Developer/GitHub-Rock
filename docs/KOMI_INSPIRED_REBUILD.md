# GitHub Rock — Komi-inspired rebuild

## Direction

GitHub Rock keeps its own name, package, OAuth flow, GitHub management features, security model, and Android-first architecture. The rebuild borrows the clarity of Komi Store's discovery experience without copying its branding, source code, assets, or product identity.

## Design principles

- Discovery-first home feed instead of a dense status dashboard
- Strong branded masthead with useful account and API health at a glance
- Segmented Recent, Popular, and Activity feeds
- Large repository cards with owner identity, ranking, language, stars, forks, issues, topics, and update time
- Minimal bottom navigation with adaptive rail support on large screens
- Clean Material 3 surfaces, dark-theme support, rounded containers, high contrast, and restrained decoration
- Honest loading, empty, offline, and error states
- Native GitHub actions stay inside the application whenever the API supports them

## Rebuild phases

### Phase 1 — Foundation

- [x] Replace the old home dashboard with a discovery feed
- [x] Add Recent, Popular, and Activity sections
- [x] Add Komi-inspired repository cards and ranking
- [x] Keep workflow monitoring and direct Builds access
- [x] Preserve pull-to-refresh and adaptive layout behavior

### Phase 2 — Repository discovery

- [ ] Add a dedicated Explore destination
- [ ] Add search history and saved filters
- [ ] Add language, repository type, visibility, and sort filters
- [ ] Add Trending and Recently Released feeds backed by real GitHub data
- [ ] Add repository action sheets for star, favourite, share, hide, and open on GitHub

### Phase 3 — Repository detail

- [ ] Rebuild the repository header with artwork, owner, stats, and primary actions
- [ ] Add Overview, Code, Issues, Pull Requests, Actions, and Releases tabs
- [ ] Add release asset compatibility labels for Android, Windows, Linux, macOS, and iOS
- [ ] Add clear loading, permission, rate-limit, and empty states per tab

### Phase 4 — Library and downloads

- [ ] Create a unified Library for favourites, starred repositories, recent repositories, and downloaded assets
- [ ] Add grouped Updates, Pending, Downloaded, and Installed sections
- [ ] Keep checksum, signing fingerprint, APK inspection, pause, resume, cancel, retry, and share controls
- [ ] Add configurable download location and mirror selection

### Phase 5 — Profile and settings

- [ ] Rebuild Profile as a compact account hub
- [ ] Group Appearance, GitHub account, Downloads, Builds, Security, Accessibility, and Advanced settings
- [ ] Keep optional feature toggles off by default
- [ ] Add theme personality presets while retaining system, light, dark, AMOLED, and dynamic colour options

## Non-goals

- Do not turn GitHub Rock into a clone of Komi Store.
- Do not remove working GitHub management features merely to match another interface.
- Do not copy Komi Store names, logos, screenshots, illustrations, or proprietary service endpoints.
- Do not replace secure native flows with untrusted web wrappers.

## Acceptance criteria

A phase is complete only when its UI, API integration, loading/error/empty states, permission checks, tests, accessibility semantics, and CI pass together.
