package com.github.sukieva.gitcommitstats.toolwindow.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.vcs.log.impl.VcsProjectLog
import com.intellij.vcs.log.visible.filters.VcsLogFilterObject
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class VcsLogNavigationService(private val project: Project) {

    private val logger = thisLogger()

    suspend fun navigateToCommit(commitHash: String) {
        try {
            logger.info("Navigating to commit: $commitHash")

            // Get Git repository
            val repository = withContext(Dispatchers.IO) {
                GitRepositoryManager.getInstance(project).repositories.firstOrNull()
            }

            if (repository == null) {
                showNotification("No Git repository found in project", NotificationType.WARNING)
                logger.warn("No Git repository found")
                return
            }

            // Navigate to commit in VCS Log
            withContext(Dispatchers.EDT) {
                val vcsProjectLog = VcsProjectLog.getInstance(project)

                // Create a hash filter for the specific commit
                val hashFilter = VcsLogFilterObject.fromHash(commitHash)
                val filters = VcsLogFilterObject.collection(hashFilter)

                // Open VCS Log tab with the hash filter
                val logUi = vcsProjectLog.openLogTab(filters)

                if (logUi != null) {
                    logger.info("Successfully navigated to commit: $commitHash in VCS Log")
                    showNotification("Opened VCS Log for commit: $commitHash", NotificationType.INFORMATION)
                } else {
                    logger.warn("Failed to open VCS Log tab for commit: $commitHash")
                    showNotification("Could not open VCS Log. Please search for hash: $commitHash", NotificationType.WARNING)
                }
            }

        } catch (e: Exception) {
            logger.error("Failed to navigate to commit: $commitHash", e)
            showNotification("Failed to navigate to commit: ${e.message}", NotificationType.ERROR)
        }
    }

    private fun showNotification(message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Git Commit Stats")
            .createNotification(message, type)
            .notify(project)
    }
}
