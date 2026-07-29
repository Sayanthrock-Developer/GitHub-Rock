# Contributing to GitHub Rock

First off, thank you for considering contributing to GitHub Rock! It's people like you that make GitHub Rock such a great tool.

## Code of Conduct

This project and everyone participating in it is governed by the [GitHub Rock Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [the issue tracker](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues).

## How Can I Contribute?

### Reporting Bugs

This section guides you through submitting a bug report for GitHub Rock. Following these guidelines helps maintainers and the community understand your report, reproduce the behavior, and find related reports.

*   **Ensure the bug was not already reported** by searching on GitHub under [Issues](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues).
*   If you're unable to find an open issue addressing the problem, [open a new one](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues/new/choose). Be sure to include a **title and clear description**, as much relevant information as possible, and a **code sample** or an **executable test case** demonstrating the expected behavior that is not occurring.

### Suggesting Enhancements

This section guides you through submitting an enhancement suggestion for GitHub Rock, including completely new features and minor improvements to existing functionality. Following these guidelines helps maintainers and the community understand your suggestion and find related suggestions.

*   **Ensure the enhancement was not already suggested** by searching on GitHub under [Issues](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues).
*   If you're unable to find an open issue addressing the problem, [open a new one](https://github.com/Sayanthrock-Developer/GitHub-Rock/issues/new/choose). Provide a **title and clear description** of the suggestion, and explain **why this enhancement would be useful** to most GitHub Rock users.

### Pull Requests

*   Please follow the title conventions when creating a PR:
    *   For performance improvements: `⚡ [performance improvement description]` (must include What, Why, and Measured Improvement).
    *   For UX improvements: `🎨 Palette: [UX improvement]` (must include What, Why, Before/After (screenshots), and Accessibility). Keep these small (<50 lines) and use existing classes.
    *   For testing improvements: `🧪 [testing improvement description]` (must include What (the gap addressed), Coverage (scenarios now tested), and Result (improvement in coverage)).
*   Do not make breaking changes.
*   Do not modify build/config files without instructions.
*   Ask before adding dependencies or making architectural changes.
*   Always add comments explaining optimizations.

## Development Environment Setup

This project uses Kotlin, Jetpack Compose, Material 3, and Gradle Kotlin DSL.

1.  Clone the repository.
2.  Open the project in Android Studio.

### Building and Testing

*   **Compile the Android app:**
    ```bash
    ./gradlew compileDebugKotlin
    ```
*   **Run unit tests:**
    ```bash
    ./gradlew testDebugUnitTest
    ```
    or
    ```bash
    ./gradlew app:test
    ```
*   **Run lint checks:**
    ```bash
    ./gradlew lint
    ```

Always run lint and test commands to verify your changes and ensure correctness before creating a Pull Request.

### Dependency Injection and Testing

The project uses Hilt/Dagger for dependency injection. It does *not* use mocking frameworks like Robolectric, MockK, or Mockito. To test classes with Android framework dependencies, refactor the code to accept dependencies via injection and implement custom Fakes for tests.

### Hygiene

Ensure repository hygiene by never leaving behind or checking in runtime-generated log files (e.g., `lint.log`, `server.log`) created during development or verification processes.

Thank you for contributing!
