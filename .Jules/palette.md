## 2024-07-28 - Tab Navigation Accessibility
**Learning:** Found static HTML site tab groups (`preview-tabs` and `workspace-tabs`) that lacked required ARIA roles and state attributes, making them difficult for screen readers to navigate as a unified component.
**Action:** Always verify that custom tab implementations include `role="tablist"`, `role="tab"`, `aria-selected`, `aria-controls`, and `role="tabpanel"` attributes to ensure keyboard and screen reader accessibility.
## 2024-07-28 - Emojis in Documentation
**Learning:** Emojis in markdown headings (e.g. `# 🎸 GitHub Rock`) provide a helpful micro-UX visual improvement by anchoring sections. When making UI/UX visual style improvements to text/markdown files like READMEs, standard Unicode emojis enhance scannability.
**Action:** Use thematic standard Unicode emojis in markdown documentation headers to match modern design and improve quick visual scanning without impacting the accessibility of the textual content.

## 2026-07-29 - Refine Login Screen Visual Polish
**Learning:** Overuse of dividers and misaligned button content (e.g. left-aligned text with icons) can make a critical authentication screen feel unpolished and fragmented. Using cohesive button styles (matching secondary actions to surrounding cards) creates a much more unified aesthetic.
**Action:** When designing vertical button stacks, ensure primary call-to-actions are perfectly centered and secondary actions share visual DNA with other contextual panels rather than relying on generic outlines. Avoid dividers unless separating radically different functional areas.
## 2026-07-30 - Modern Header Icons
**Learning:** Using circular background containers for icons next to headers significantly elevates the visual hierarchy and brings designs closer to modern Material 3 standard implementations.
**Action:** When adding icons to major section headers (like README.md), use a Surface with a primaryContainer color and CircleShape to frame the icon cleanly.
## 2025-02-17 - Compose accessibility for metric icon-text rows
**Learning:** In Compose, an icon and text row (like a star icon with '1.2k') is normally read by screen readers as separate, out-of-context items. We can group them by applying `Modifier.clearAndSetSemantics { contentDescription = ... }` on the container.
**Action:** Use `clearAndSetSemantics` with a descriptive `contentDescription` when building simple icon-and-text metrics so that the screen reader announces the combined meaning clearly.

## 2026-08-04 - Compose Accessibility for Metric Icons
**Learning:** For small metric items that pair an icon with a text label/value (like a repository meta pill), using standard `Modifier.semantics` can sometimes still allow screen readers to read the icon and text as separate or disjointed items. Using `Modifier.clearAndSetSemantics` on the parent container ensures the screen reader completely overrides the individual child semantics and announces the metric as a single, clear, combined piece of information.
**Action:** When building custom icon-and-text metrics or pills in Jetpack Compose, use `Modifier.clearAndSetSemantics` with a single `contentDescription` on the container to combine the meaning into one clear announcement.
## 2024-05-18 - Render markdown images
**Learning:** The custom markdown parser actively stripped image tags `![alt](url)` and converted them to plain text, severely affecting the UX of documentation pages. Standard Android views for markdown support image tags, so any custom implementation needs to handle parsing properly to keep par with desktop tools.
**Action:** Created a new `Image` block kind and extracted the url in the markdown parser (`MarkdownRenderer.kt`), ensuring it does not get cleared by subsequent line parsers. Updated Compose screen blocks (`RepositoryShowcaseScreen.kt`, `RepositoryHubContent.kt`, `RepositoryDetailScreen.kt`) to catch the image `MarkdownBlockKind` and load them gracefully with Coil's `AsyncImage` modifier filling the layout width `Modifier.fillMaxWidth()` for an immersive reading experience without horizontal scrollbars cutting off documentation image content.

## 2026-08-09 - Markdown Image ContentScale Issue
**Learning:** In Jetpack Compose, using `ContentScale.Inside` with `Modifier.fillMaxWidth()` inside infinitely tall scrollable containers (like `LazyColumn` or `verticalScroll`) causes `AsyncImage` layouts to fail when rendering SVGs or images without resolved intrinsic boundaries, leading to blank screens.
**Action:** Always prefer `ContentScale.FillWidth` (or fixed aspect ratios) for remote markdown images wrapped inside scrollable layouts to ensure rendering consistency and prevent layout collapse.
