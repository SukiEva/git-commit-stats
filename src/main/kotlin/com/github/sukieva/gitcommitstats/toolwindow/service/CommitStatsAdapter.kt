package com.github.sukieva.gitcommitstats.toolwindow.service

import com.github.sukieva.gitcommitstats.stats.CommitStats
import com.github.sukieva.gitcommitstats.stats.CommitStatsCalculator
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.changes.Change
import com.intellij.vcsUtil.VcsUtil
import git4idea.GitCommit
import git4idea.GitContentRevision
import git4idea.GitRevisionNumber

class CommitStatsAdapter(private val project: Project) {

    private val calculator = CommitStatsCalculator()
    private val logger = thisLogger()

    suspend fun computeStatsForCommit(commit: GitCommit): CommitStats {
        return try {
            val changes = convertToChanges(commit)
            calculator.computeStats(changes)
        } catch (e: Exception) {
            logger.warn("Failed to compute stats for commit ${commit.id.toShortString()}", e)
            // Return partial stats with file count only
            CommitStats(filesModified = commit.changes.size)
        }
    }

    private fun convertToChanges(commit: GitCommit): List<Change> {
        val changes = mutableListOf<Change>()

        for (change in commit.changes) {
            try {
                changes.add(change)
            } catch (e: Exception) {
                logger.debug("Failed to process change in commit ${commit.id.toShortString()}", e)
            }
        }

        return changes
    }
}
