package com.github.sukieva.gitcommitstats.toolwindow.service

import com.github.sukieva.gitcommitstats.stats.CommitStatsCalculator
import com.github.sukieva.gitcommitstats.toolwindow.model.CommitWithStats
import com.github.sukieva.gitcommitstats.toolwindow.model.FileHotspot
import com.intellij.openapi.diagnostic.thisLogger
import git4idea.GitCommit

class FileHotspotAnalyzer {

    private val logger = thisLogger()
    private val calculator = CommitStatsCalculator()

    fun analyzeHotspots(commits: List<CommitWithStats>, gitCommits: List<GitCommit>, topN: Int = 10): List<FileHotspot> {
        val fileStats = mutableMapOf<String, Pair<Int, Int>>() // path -> (count, totalLines)

        gitCommits.forEach { commit ->
            commit.changes.forEach { change ->
                val filePath = change.afterRevision?.file?.path ?: change.beforeRevision?.file?.path
                if (filePath != null) {
                    val (count, totalLines) = fileStats.getOrDefault(filePath, Pair(0, 0))

                    // Calculate actual lines changed for this file
                    val linesChanged = try {
                        val fileStats = calculator.computeStatsForChange(change)
                        fileStats.linesAdded + fileStats.linesDeleted
                    } catch (e: Exception) {
                        logger.warn("Failed to compute stats for file $filePath in commit ${commit.id.toShortString()}", e)
                        // Fallback to estimate if calculation fails
                        when {
                            change.beforeRevision == null -> 100 // New file
                            change.afterRevision == null -> 100 // Deleted file
                            else -> 50 // Modified file
                        }
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
