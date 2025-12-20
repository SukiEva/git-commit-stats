package com.github.sukieva.gitcommitstats.toolwindow.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class AuthorService(private val project: Project) {

    private val logger = thisLogger()
    private var cachedAuthors: List<String>? = null

    suspend fun getAuthors(): List<String> = withContext(Dispatchers.IO) {
        if (cachedAuthors != null) {
            return@withContext cachedAuthors!!
        }

        try {
            val repository = GitRepositoryManager.getInstance(project)
                .repositories.firstOrNull()

            if (repository == null) {
                logger.warn("No Git repository found in project")
                return@withContext emptyList()
            }

            val root = repository.root

            // Load commits to extract unique authors
            val commits = try {
                val allCommits = GitHistoryUtils.history(project, root)
                // Limit to recent commits for performance (take first 1000)
                allCommits.take(1000)
            } catch (e: Exception) {
                logger.warn("Failed to load commit history for authors", e)
                emptyList()
            }

            val authors = commits
                .mapTo(mutableSetOf()) { it.author.name }
                .filter { it.isNotBlank() }
                .sorted()

            cachedAuthors = authors

            logger.info("Loaded ${authors.size} unique authors")
            authors
        } catch (e: Exception) {
            logger.error("Failed to load authors", e)
            emptyList()
        }
    }

    fun invalidateCache() {
        cachedAuthors = null
    }
}
