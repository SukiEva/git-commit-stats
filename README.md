# GitCommitStats

![Build](https://github.com/SukiEva/git-commit-stats/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/29370.svg)](https://plugins.jetbrains.com/plugin/29370)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/29370.svg)](https://plugins.jetbrains.com/plugin/29370)

An IntelliJ IDEA plugin that provides real-time statistics for uncommitted changes directly in your status bar.

<!-- Plugin description -->
**GitCommitStats** provides real-time visibility into your uncommitted changes directly in the status bar. Know exactly what you're about to commit before you hit that commit button.

## Features

- **Real-time Statistics**: See file and line change counts update instantly as you select or deselect files in the commit dialog
- **File Change Tracking**: Track modified, added, and deleted files with accurate counts
- **Line Change Analysis**: View additions (+) and deletions (-) across all selected changes
- **Binary File Detection**: Automatically identifies and counts binary files separately
- **Non-Intrusive UI**: Displays statistics in the status bar with a clean, informative format
- **Detailed Tooltips**: Hover over the status bar widget for a complete breakdown of your changes

## Compatibility

Compatible with IntelliJ 2025.3+.
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "git-commit-stats"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/29370) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/29370/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/SukiEva/git-commit-stats/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Usage

1. **Open the Commit Dialog**: Use <kbd>Ctrl+K</kbd> (Windows/Linux) or <kbd>⌘K</kbd> (macOS) to open the commit dialog, or click the commit button in the toolbar

2. **View Statistics**: Look at the bottom status bar. When the commit dialog is open, you'll see a widget displaying:
   - File count (e.g., "3 files")
   - Line changes (e.g., "+42/-15")
   - A modification icon to indicate active changes

3. **Interactive Updates**: As you select or deselect files in the commit dialog, the statistics update automatically to reflect only the selected changes

4. **Detailed Breakdown**: Hover over the status bar widget to see a tooltip with detailed information:
   - Number of modified files
   - Number of added files
   - Number of deleted files
   - Number of binary files
   - Total lines added
   - Total lines deleted

5. **Stay Informed**: Use these statistics to ensure your commits are appropriately scoped and organized

### Example Display Formats

- `1 file, +10/-2` - Single file with 10 additions and 2 deletions
- `5 files, +150/-80` - Multiple files with line change summary
- `3 files` - Files with no text changes (e.g., binary files only)

## Features in Detail

### Real-time Statistics
The plugin computes statistics dynamically as you interact with the commit dialog. Select or deselect files, and watch the numbers update instantly.

### Accurate Line Counting
Using IntelliJ's built-in diff engine, the plugin provides accurate line-by-line change analysis, respecting the same comparison rules as the IDE's diff viewer.

### Performance Optimized
- **Async Computation**: Statistics are calculated in background threads to keep the UI responsive
- **Smart Debouncing**: Rapid selection changes are intelligently batched to prevent unnecessary recalculations
- **Graceful Degradation**: For extremely large diffs, the plugin shows file counts while skipping detailed line analysis

### VCS Agnostic Design
The plugin works with any version control system supported by IntelliJ IDEA, including:
- Git
- Subversion (SVN)
- Mercurial
- Perforce
- And any other VCS with IntelliJ support

## Development

For development information, build commands, and architecture details, see [CLAUDE.md](CLAUDE.md).

## License

This plugin is based on the [IntelliJ Platform Plugin Template][template].

## Support

If you encounter any issues or have feature requests, please visit the [GitHub Issues](https://github.com/SukiEva/git-commit-stats/issues) page.

---

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
