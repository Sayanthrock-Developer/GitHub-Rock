## 2026-07-28 - Optimize O(N^2) span overlap checks

**Learning:** Checking for span overlaps by iterating through an entire list of previously accepted items yields O(N^2) complexity, leading to noticeable performance degradation on larger inputs (e.g., long source files).
**Action:** When candidates are sorted primarily by their start index, optimize overlaps verification by maintaining a `maxEnd` pointer. A new candidate only needs to verify `candidate.start >= maxEnd` (an O(1) comparison), eliminating the nested loop and guaranteeing that no overlaps exist.
