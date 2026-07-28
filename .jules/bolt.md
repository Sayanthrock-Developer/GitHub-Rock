## 2024-05-24 - LazyColumn indexOf Performance Anti-Pattern
**Learning:** Found O(N^2) complexity anti-pattern in Compose lists when using `items(list)` and computing indices via `list.indexOf(item)`. This happens on every list item recomposition.
**Action:** Use `itemsIndexed(list)` for Compose lists whenever an item's index is required within the view logic, saving an O(N) lookup.
