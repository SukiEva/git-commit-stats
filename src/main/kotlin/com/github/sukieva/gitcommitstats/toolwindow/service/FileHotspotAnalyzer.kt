package com.github.sukieva.gitcommitstats.toolwindow.service

import com.github.sukieva.gitcommitstats.toolwindow.model.CommitWithStats
import com.github.sukieva.gitcommitstats.toolwindow.model.FileHotspot
import git4idea.GitCommit

class FileHotspotAnalyzer {

    fun analyzeHotspots(commits: List<CommitWithStats>, gitCommits: List<GitCommit>, topN: Int = 10): List<FileHotspot> {
        val fileStats = mutableMapOf<String, Pair<Int, Int>>() // path -> (count, totalLines)

        gitCommits.forEach { commit ->
            commit.changes.forEach { change ->
                val filePath = change.afterRevision?.file?.path ?: change.beforeRevision?.file?.path
                if (filePath != null) {
                    val (count, totalLines) = fileStats.getOrDefault(filePath, Pair(0, 0))

                    // Estimate lines changed (this is approximate)
                    val linesChanged = when {
                        change.beforeRevision == null -> 100 // New file, assume 100 lines
                        change.afterRevision == null -> 100 // Deleted file, assume 100 lines
                        else -> 50 // Modified file, assume 50 lines average
                    }

                    fileStats[filePath] = Pair(count + 1, totalLines + linesChanged)
                }
            }
        }

        return fileStats.entries
            .map { (path, stats) -> FileHotspot(path, stats.first, stats.second) }
            .sortedByDescending { it.modificationCount }
            .take(topN)
    }
}
