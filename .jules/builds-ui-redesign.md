# GitHub Rock — Builds Layered UI Redesign

## Goal
Redesign Builds so build details open as an in-page expandable layer instead of forcing a separate full-page navigation flow. Keep the existing GitHub Actions functionality intact while making build status, logs, jobs, steps, artifacts, and actions reachable with minimal taps.

## UX contract
- Builds remains the primary screen.
- Tapping a recent workflow run opens a native Compose detail layer/sheet over Builds.
- The layer supports collapsed, expanded and full-height states and closes with back/gesture without losing Builds scroll position, repository selection, filters, or state.
- Do not replace working native GitHub API functionality with WebView links.
- Use the existing Rock glass/standard components and AppearancePreferences.

## Layer contents
1. Header: workflow title, status, repository, branch, run number.
2. Compact status summary: queued/running/success/failure/cancelled.
3. Quick actions: refresh, re-run where supported, cancel while running where supported, open GitHub URL, copy URL.
4. Jobs list with expandable steps.
5. Workflow logs with the existing WorkflowLogViewer.
6. Artifacts with existing Downloads enqueue behavior.
7. Commit/branch/workflow context.
8. Clear permission/error/offline states.

## Builds landing screen
- Keep repository selector, workflow preview, dispatch controls and recent runs.
- Add a compact segmented filter: All / Running / Failed / Success.
- Add a small summary row for Running, Failed, Success and Artifacts.
- Recent run cards must expose a clear tap target and status.
- Preserve compact-card preference and status-color preference.
- Use stable keys and avoid unnecessary recomposition.

## Motion
- Use a short fade + vertical expansion for the layer.
- Respect `AppearancePreferences.reduceMotion`.
- Do not use excessive animation or blur that harms readability.

## Accessibility
- Every icon-only action needs contentDescription.
- Minimum 48dp touch targets.
- State must be communicated by text/icon, not color alone.
- Support large font sizes and screen readers.

## Implementation guidance
- Prefer `ModalBottomSheet`/Compose sheet or an anchored in-page layer depending on the existing navigation architecture.
- Reuse `BuildsViewModel`, `WorkflowLogViewer`, `DownloadsViewModel`, `RunFrame`, `StatusRow`, `GlassCard`, and existing workflow models rather than duplicating API logic.
- If a run is selected from Recent Runs, update the selected repository/run state and load its details through the existing ViewModel path.
- Keep web fallback only for GitHub capabilities that are genuinely unavailable through the app/API.

## Acceptance criteria
- A recent build can be opened, inspected and dismissed without leaving Builds.
- Jobs and steps are expandable.
- Logs and artifacts remain accessible from the same layer.
- Running builds visibly refresh and expose valid actions.
- Dispatch, refresh and existing workflow preview continue working.
- Back gesture closes the layer before navigating away from Builds.
- Existing Builds tests are updated/extended for the new interaction.
- Android build and tests pass before merge.
