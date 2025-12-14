# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an IntelliJ Platform plugin project called "git-commit-stats" built using the IntelliJ Platform Plugin Template. The plugin is currently in its initial template state with sample code that should be removed as actual features are implemented.

**Plugin Details:**
- Group: `com.github.sukieva.gitcommitstats`
- Main package: `com.github.sukieva.gitcommitstats`
- Target platform: IntelliJ IDEA 2025.2.5
- Minimum build: 252
- Language: Kotlin (JVM toolchain 21)

## Build Commands

Build the plugin:
```bash
./gradlew buildPlugin
```

Run tests:
```bash
./gradlew check
```

Run the plugin in a sandboxed IDE instance:
```bash
./gradlew runIde
```

Run UI tests with robot server:
```bash
./gradlew runIdeForUiTests
```

Verify plugin compatibility:
```bash
./gradlew verifyPlugin
```

Run Qodana code inspections:
```bash
./gradlew qodana
```

Generate test coverage reports:
```bash
./gradlew koverReport
```

Get changelog (for release notes):
```bash
./gradlew getChangelog --unreleased --no-header
```

## Testing

Run all tests:
```bash
./gradlew check
```

Run a single test class:
```bash
./gradlew test --tests "com.github.sukieva.gitcommitstats.MyPluginTest"
```

Test coverage is tracked using Kover and uploaded to CodeCov in CI.

## Architecture

### Plugin Structure

The plugin integrates with IntelliJ's commit workflow using the CheckinHandler extension point:

**Checkin Integration** (`src/main/kotlin/com/github/sukieva/gitcommitstats/checkin/`):
- `CommitStatsHandlerFactory` - Factory registered in plugin.xml as checkinHandlerFactory
- `CommitStatsHandler` - Extends CheckinHandler, manages lifecycle and updates UI
- Hooks into commit panel via `getBeforeCheckinConfigurationPanel()`
- Responds to file selection changes via `includedChangesChanged()`

**Statistics Engine** (`src/main/kotlin/com/github/sukieva/gitcommitstats/stats/`):
- `CommitStatsCalculator` - Core logic for computing diff statistics
- Uses IntelliJ's `ComparisonManager` API for accurate line counting
- Handles binary files, errors, and cancellation
- Async computation with Kotlin coroutines

**UI Components** (`src/main/kotlin/com/github/sukieva/gitcommitstats/ui/`):
- `CommitStatsPanel` - Displays file and line statistics
- Updates dynamically when commit selection changes
- Shows: modified/added/deleted files, lines added/removed, binary file count

**Localization**:
- `MyBundle.kt` - Bundle accessor using DynamicBundle
- Message keys stored in `src/main/resources/messages/MyBundle.properties`

### Configuration Files

**plugin.xml** (`src/main/resources/META-INF/plugin.xml`):
- Declares checkinHandlerFactory extension point
- Depends on com.intellij.modules.vcs for VCS integration
- Plugin description is extracted from README.md during build (between `<!-- Plugin description -->` markers)

**build.gradle.kts**:
- Uses IntelliJ Platform Gradle Plugin 2.x
- Configured for signing, publishing, and verification
- Change notes extracted from CHANGELOG.md
- Publishing to custom channels based on version suffix (e.g., "alpha", "beta")

**gradle.properties**:
- Central configuration for plugin metadata, version, and platform version
- Modify pluginGroup, pluginName, pluginVersion here
- Includes VCS module dependency: `platformBundledModules = intellij.platform.vcs`

## Development Workflow

1. When adding new functionality:
   - Register new extension points in plugin.xml
   - Follow the existing package structure under `com.github.sukieva.gitcommitstats`
   - Use Kotlin coroutines for background computation
   - Update UI on EDT thread via `withContext(Dispatchers.EDT)`
2. Plugin description in README.md between markers is used as the official description
3. CHANGELOG.md entries are used for release notes
4. Statistics computation uses IntelliJ's `ComparisonManager` for diff analysis

## CI/CD

The project uses GitHub Actions with three workflows:

**build.yml**: Runs on push/PR
- Builds plugin
- Runs tests with coverage (uploads to CodeCov)
- Runs Qodana inspections
- Verifies plugin structure
- Creates draft release

**release.yml**: Triggered on published release
- Builds and publishes to JetBrains Marketplace

**run-ui-tests.yml**: UI testing workflow

## Important Notes

- Gradle configuration cache is enabled
- Kotlin stdlib is not bundled by default (kotlin.stdlib.default.dependency = false)
- Java 21 is required
- The plugin depends on `com.intellij.modules.platform` and `com.intellij.modules.vcs`
- Statistics are computed asynchronously with 300ms debouncing
- VCS-agnostic implementation works with Git, SVN, Mercurial, etc.
