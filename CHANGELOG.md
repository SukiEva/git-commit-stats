<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# git-commit-stats Changelog

## [Unreleased]

### Added

- **File Hotspot Analysis**: New "File Hotspots" tab showing top 10 most frequently modified files
  - Displays modification count and total lines changed per file
  - Helps identify code hotspots and potential problem areas
  - Double-click navigation to filter commits that modified the file

- **Large Commit Detection**: Visual warnings and checks for commits exceeding 500 lines
  - Yellow background highlighting in commit table
  - Warning icon with tooltip in the lines column
  - Optional commit check that prompts confirmation before committing large changes
  - Checkbox in Commit Checks panel to enable/disable the check

- **Enhanced UI Interactions**:
  - Double-click hotspot files to jump to filtered commit list
  - Tooltip on large commit warning icon explaining the threshold
  - Author dropdown state preservation across UI updates

### Fixed

- **Author Dropdown**: Fixed state loss when UI updates
  - Selection now preserved during data refresh
  - Added proper state management with isUpdating flag
  - Initial empty item to prevent blank display

- **Accurate Line Counting**: File hotspots now use real diff calculations
  - Replaced rough estimates with actual line additions/deletions
  - Provides accurate code churn metrics per file

## [1.3.0] - 2025-12-26

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

[Unreleased]: https://github.com/SukiEva/git-commit-stats/compare/v1.3.0...HEAD
[1.3.0]: https://github.com/SukiEva/git-commit-stats/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/SukiEva/git-commit-stats/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/SukiEva/git-commit-stats/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/SukiEva/git-commit-stats/commits/v1.0.0
