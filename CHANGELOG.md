<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# git-commit-stats Changelog

## [Unreleased]

### Added

- **Author and Email Columns**: Added author and email columns to the commit list table for better commit attribution visibility
- **Auto-open Diff View**: Double-clicking a commit now automatically shows the diff view without requiring manual selection

### Changed

- **Default Date Range**: Tool window now defaults to showing today's commits instead of the last 3 months for faster initial loading

## [1.2.0] - 2025-12-21

### Added

- **Double-click Navigation**: Double-click on any commit in the Tool Window to open and navigate to that commit in IDEA's VCS Log window
- Hash-filtered VCS Log tab automatically opens and highlights the selected commit for detailed inspection

## [1.1.0]

### Added

- **Git Commit Statistics Tool Window**: New tool window panel for comprehensive commit analysis
- Summary statistics panel showing total commits, files changed, and lines modified
- Detailed commit list with sortable columns (Hash, Date, Message, Files, Lines)

## [1.0.0]

### Added

- File counts (modified, added, deleted)
- Line counts (additions, deletions)
- Binary file detection

[Unreleased]: https://github.com/SukiEva/git-commit-stats/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/SukiEva/git-commit-stats/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/SukiEva/git-commit-stats/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/SukiEva/git-commit-stats/commits/v1.0.0
