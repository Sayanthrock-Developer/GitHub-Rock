# Reference feature integration: Copilot CLI, Copilot app, and gh-ost

This document records how GitHub Rock may adapt useful workflows from three public reference projects without copying their branding, proprietary implementation, or platform-specific behaviour:

- [`github/copilot-cli`](https://github.com/github/copilot-cli)
- [`github/app`](https://github.com/github/app)
- [`github/gh-ost`](https://github.com/github/gh-ost)

Tracking issue: [#172](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues/172)

Reference baseline reviewed: **26 July 2026**.

## Decision

GitHub Rock can add the high-value product workflows, but it cannot honestly provide every capability in the same way on Android.

| Capability | Correct GitHub Rock home |
| --- | --- |
| GitHub issues, pull requests, workflows, releases, files, review state, and My Work | Native Android with GitHub authorization |
| Agent session UI, cloud agents, scheduled agent work, provider-backed AI | Native Android client plus trusted backend/provider |
| Local git worktrees, shell, terminal attachment, LSP processes, IDE focus, local plugins | Desktop companion |
| MCP configuration and remote MCP connections | Android UI plus trusted backend; local MCP processes in desktop companion |
| MySQL topology access and `gh-ost` execution | Trusted operations backend or desktop/server companion |
| Monitoring and typed control of approved remote jobs | Native Android client through an authenticated allow-listed API |

The Android APK must never contain shared private keys, GitHub App installation tokens, model-provider keys, unrestricted shell credentials, MySQL passwords, database TLS private keys, webhook secrets, or long-lived backend credentials.

## Product principles

1. **Adapt workflows, do not clone products.** Use GitHub Rock branding, navigation, architecture, and permission model.
2. **No false parity.** A visible item remains `Roadmap`, `Backend-dependent`, or `Companion-only` until its implementation and tests are merged.
3. **Explicit approval.** File writes, commands, broad tool approval, automation activation, merge, database migration execution, cut-over, and abort require clear confirmation.
4. **Typed operations.** Android sends validated structured requests, never arbitrary REST paths, GraphQL documents, shell strings, socket commands, hosts, or file paths.
5. **Least privilege.** Every integration uses minimum GitHub, provider, operating-system, and database permissions.
6. **Auditable behaviour.** Record actor, target, requested action, result, and timestamp without recording secret values.

## Reference 1: GitHub Copilot CLI

The reference CLI combines natural-language coding assistance, GitHub context, sessions, tools, extensibility, terminal workflows, and explicit action approval.

### Features to adapt

#### Sessions and conversation

- Create, rename, resume, restart, archive, close, and search sessions.
- Preserve sessions across restarts.
- Keep each session tied to its repository or working directory.
- Multi-session navigation and background session state.
- Task and subagent timelines with prompt origin, progress, cancellation, steering, and retained results.
- Prompt history, pinned prompts, templates, sharing, HTML/file export, and safe Gist export.
- Repository, issue, pull request, commit, workflow, file, and Gist references in the composer.

#### Agent modes and model controls

- Ask mode for explanation and guided work.
- Plan mode that blocks workspace mutations.
- Autopilot mode with explicit continuation limits and stop controls.
- Model picker with model availability, reasoning effort, context tier, and per-session overrides.
- Apply model/context changes safely when an active session reaches an idle boundary.
- Usage and provider-cost presentation only when accurate provider data is available.

#### Tools and approvals

- Preview tool intent and target before execution.
- Allow once, deny, or always allow only a narrow reviewed rule.
- Session-wide approval controls with a persistent warning state.
- `/allow-all-tools on|off|show` and `/yolo on|off|show` aliases behind the same safety model.
- Per-repository approval isolation so approval does not leak after switching projects.
- Tool search, grouped tool progress, failure details, retry, and cancellation.

#### Workspace, shell, and sandbox

- Repository/folder trust confirmation.
- Isolated worktree sessions.
- Move current changes into a new worktree through an explicit operation.
- Terminal attach/open/read/stop/retry with retained output.
- OS sandbox state and filesystem/network policy summary.
- Git and GitHub authentication inside the sandbox only when explicitly enabled.
- Additional-directory context with reviewed access.
- Low-storage warnings and workspace cleanup.

These processes belong in the desktop companion. Android may display status and request approved actions through a typed backend contract, but it must not execute unrestricted local shell commands.

#### MCP, LSP, plugins, skills, and hooks

- MCP server add/edit/delete, status, logs, OAuth, reconnect, resources, tools, enable/disable, and trusted repository scope.
- LSP configuration, server status, diagnostics, go-to-definition metadata, hover data, and controlled rename/edit previews.
- Plugin manifests, plugin marketplaces, install/update/uninstall, enable/disable, and source trust.
- Skills at user or repository scope with model-invocation policy.
- Custom agents with validated names, tools, model policy, and source labels.
- Lifecycle/tool hooks with timeout, failure policy, recursion/loop protection, and audit history.
- Extension-driven canvases and inspectable panels.

Local MCP/LSP/plugin processes run in the desktop companion. A trusted backend may run remote integrations. Android stores only non-secret configuration and short-lived user session material appropriate to the mobile client.

#### Quality-of-life and accessibility

- Searchable slash-command palette and contextual help.
- Hardware-keyboard navigation.
- Voice input with explicit microphone permission and visible recording state.
- Screen-reader announcements for focused rows, tool state, copy actions, errors, and results.
- Reduced motion, large text, switch access, contrast, and focus-ring validation.
- Debug-log collection with secret redaction preview and explicit sharing confirmation.

## Reference 2: GitHub Copilot app

The reference desktop app organises parallel agent-driven work across repositories with My Work, isolated sessions, canvases, automations, pull-request review, and merge guidance.

### Features to adapt

#### My Work

Create a native attention centre that combines:

- assigned issues
- review-requested pull requests
- authored pull requests
- mentions and updates
- workflow/check failures
- saved or recently viewed repositories
- local and cloud sessions
- automation runs

Required behaviour:

- repository/account filters
- saved sections and sorting
- update/unread indicators
- cached-first loading and background refresh
- clear retry states instead of raw server content
- permission-aware actions
- state preservation after repository rename or transfer

#### Parallel sessions

- Local, cloud, backend, and companion session labels.
- Isolated workspace/worktree identity.
- Create a new session or hand a GitHub issue/PR link to an existing session.
- Preserve the current session while opening side work.
- Restart failed resume operations.
- Prevent late events from a previous session appearing in the active session.
- Remove abandoned background agents from active UI.

#### Canvases and workspace panels

Use inspectable panels instead of hiding everything in chat:

- Plan
- Conversation
- Tasks/subagents
- Terminal
- Files
- Changes/diff
- Pull request
- Issue
- Workflow/checks
- Browser/preview where supported
- Extensions

Each panel needs independent loading, empty, error, persistence, keyboard navigation, and accessibility state.

#### Pull-request review and Agent Merge

- Commit navigation and per-commit diffs.
- Incremental loading for large pull requests.
- Mark/unmark files reviewed and show reviewed/total progress.
- Draft reviews, comments, review-thread state, and correct submitting account.
- Required reviews, unresolved threads, required checks, merge queue/state, branch protection, rulesets, and merge eligibility.
- Local and remote uncommitted-change summary where a companion workspace exists.
- Hide rerun or merge controls when GitHub does not permit them.
- Never bypass repository protections.

#### Automations

- Daily and weekly quarter-hour schedules.
- Event-based runs only through a verified trusted backend.
- Prompt template, repository scope, model/provider, tool policy, timeout, and usage guardrails.
- Enable/disable, queued/running/succeeded/failed/cancelled states, live duration, retry, cancel, and logs.
- Exact Last run and Next run timestamps with accessible labels.
- Unsaved-change warning in editors.
- Cloud automation secrets remain in a backend secret manager.

#### Extensions and Files

- Files panel with filter autofocus, keyboard selection, open, path, and persistence.
- Trusted GitHub folder or Gist extension installation with review before activation.
- Compact add-panel menu with an Extensions group.
- Storage warnings and session storage breakdown.
- Right-panel availability for sessions that already contain a diff or pull request.

## Reference 3: gh-ost

`gh-ost` is a production MySQL online schema migration engine. It is not a GitHub mobile feature and must not run with production database credentials inside the APK.

GitHub Rock may provide a **remote MySQL migration centre** that controls an approved `gh-ost` service through a typed API.

### Migration modes

- Noop validation without `--execute`.
- Test on replica.
- Migrate on replica.
- Replica-assisted migration on the primary.
- Direct-primary migration only after explicit topology validation and approval.

The UI must explain which hosts are inspected, where rows are read/written, where binlogs are read, and where cut-over occurs.

### Preflight and review

- Backend identity and certificate verification.
- Approved environment/project/database allow-list.
- Topology, replication, binlog format, table keys, unsupported setup, and concurrent-operation checks.
- Schema, table, ALTER statement, estimated row count, selected mode, and risk summary.
- Dry-run/noop first recommendation.
- Secretless Android request; credentials are resolved only in the backend secret manager.

### Progress and auditing

Display typed fields for:

- copied rows and percentage
- applied binary-log events
- backlog
- binlog coordinates
- inspector and applier identity
- replication and heartbeat lag
- elapsed copy/total time
- ETA
- throttling reason
- migration state
- hooks and audit events
- replica-test comparison/checksum result

### Runtime controls

Expose only validated operations:

- status and brief status
- pause/throttle
- resume/no-throttle
- chunk size
- DML batch size
- maximum replication lag
- maximum load thresholds
- critical load thresholds
- nice ratio
- throttle endpoint/query/control replicas
- postpone cut-over
- separately confirmed unpostpone/cut-over
- emergency abort with typed confirmation

Android must never send arbitrary text directly to a Unix socket or TCP control port. The backend maps typed requests to approved `gh-ost` commands and validates the active migration, actor, database, table, and allowed value range.

### Safety rules

- Do not run concurrent migrations on the same table.
- Use unique control endpoints and server IDs where required.
- Redact passwords, DSNs, certificate material, environment values, and command arguments.
- Use disposable MySQL containers and replicas for integration tests.
- Treat abort, cut-over, direct-primary mode, and `--execute` as high-risk confirmed actions.
- Keep audit records without storing database secrets.

## Delivery phases

### Phase A — Native My Work foundation

- Native attention centre.
- GitHub reference parser and hand-off actions.
- Cached-first issue/PR/workflow detail.
- Permission-aware actions and recovery states.

### Phase B — Session and agent client

- Session models, navigation, composer, task timeline, modes, model settings, and approvals.
- Provider/backend contract with honest unavailable states.
- No unrestricted tools in Android.

### Phase C — Desktop workspace companion

- Worktrees, terminal, sandbox, IDE hand-off, local MCP/LSP/plugins, and workspace storage management.

### Phase D — Extensibility and canvases

- MCP, plugins, skills, hooks, extensions, and inspectable workspace panels.

### Phase E — Automations and Agent Merge

- Scheduled/background work, run history, review/check/merge readiness, and repository protection enforcement.

### Phase F — gh-ost operations backend

- Typed API, secret manager, disposable integration environment, migration dashboard, dynamic controls, auditing, and incident recovery.

## Definition of done

A capability may be marked supported only when all applicable requirements pass:

- correct platform label
- working implementation
- minimum permissions
- loading, empty, error, offline, and recovery states
- accessibility
- unit and UI tests
- backend/companion contract tests where required
- security and secret-redaction tests
- CI evidence
- release documentation

Do not use a checked box, `Ready` badge, enabled action, or success message as a substitute for implementation evidence.