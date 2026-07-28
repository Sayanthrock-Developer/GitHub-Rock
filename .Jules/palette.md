## 2024-07-28 - Tab Navigation Accessibility
**Learning:** Found static HTML site tab groups (`preview-tabs` and `workspace-tabs`) that lacked required ARIA roles and state attributes, making them difficult for screen readers to navigate as a unified component.
**Action:** Always verify that custom tab implementations include `role="tablist"`, `role="tab"`, `aria-selected`, `aria-controls`, and `role="tabpanel"` attributes to ensure keyboard and screen reader accessibility.
