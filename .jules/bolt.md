## 2024-05-24 - LazyColumn indexOf Performance Anti-Pattern
**Learning:** Found O(N^2) complexity anti-pattern in Compose lists when using `items(list)` and computing indices via `list.indexOf(item)`. This happens on every list item recomposition.
**Action:** Use `itemsIndexed(list)` for Compose lists whenever an item's index is required within the view logic, saving an O(N) lookup.

## 2024-05-18 - Concurrent GraphQL requests with Kotlin Coroutines
**Learning:** Sequential processing of chunked requests (like GraphQL batching) can introduce significant latency, commonly referred to as N+1 query problem behavior. Utilizing Kotlin Coroutines with `coroutineScope`, `async`, and `awaitAll()` allows for parallel execution of these batches, drastically reducing total response time (e.g., from ~500ms to ~100ms for multiple simulated requests).
**Action:** When handling lists of network requests or batching operations, prioritize parallel execution using Kotlin Coroutines where thread-safety (like a `synchronizedMap`) is guaranteed.
