package com.github.sukieva.gitcommitstats.toolwindow.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import git4idea.GitCommit
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

@Service(Service.Level.PROJECT)
class GitQueryService(private val project: Project) {

    private val logger = thisLogger()

    suspend fun fetchCommits(
        author: String?,
        startDate: Date?,
        endDate: Date?
    ): List<GitCommit> = withContext(Dispatchers.IO) {
        try {
            val repository = GitRepositoryManager.getInstance(project)
                .repositories.firstOrNull()

            if (repository == null) {
                logger.warn("No Git repository found in project")
                return@withContext emptyList()
            }

            val root = repository.root

            // Fetch all commits using GitHistoryUtils
            val allCommits = try {
                // Get commits from all branches by using the current branch
                GitHistoryUtils.history(project, root)
            } catch (e: Exception) {
                logger.warn("Failed to fetch commit history", e)
                emptyList()
            }

            // Filter commits by author and date range
            val (normalizedStart, normalizedEnd) = normalizeDateRange(startDate, endDate)
            val filteredCommits = allCommits.filter { commit ->
                // Filter by author - support partial match and case-insensitive
                if (author != null && author.isNotEmpty()) {
                    val authorLower = author.lowercase()
                    val nameMatches = commit.author.name.lowercase().contains(authorLower)
                    val emailMatches = commit.author.email.lowercase().contains(authorLower)
                    if (!nameMatches && !emailMatches) {
                        return@filter false
                    }
                }

                // Filter by date range
                val commitDate = Date(commit.commitTime)
                if (normalizedStart != null && commitDate.before(normalizedStart)) {
                    return@filter false
                }
                if (normalizedEnd != null && commitDate.after(normalizedEnd)) {
                    return@filter false
                }

                true
            }

            logger.info("Fetched ${filteredCommits.size} commits for author=$author, dateRange=[$startDate, $endDate]")
            filteredCommits
        } catch (e: Exception) {
            logger.error("Failed to fetch commits", e)
            emptyList()
        }
    }
}

internal fun normalizeDateRange(startDate: Date?, endDate: Date?): Pair<Date?, Date?> {
    if (startDate == null && endDate == null) {
        return Pair(null, null)
    }

    val startCal = startDate?.let {
        java.util.Calendar.getInstance().apply {
            time = it
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
    }

    val endCal = endDate?.let {
        java.util.Calendar.getInstance().apply {
            time = it
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }
    }

    return Pair(startCal?.time, endCal?.time)
}
