package com.github.sukieva.gitcommitstats.checkin

import com.github.sukieva.gitcommitstats.MyBundle
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.components.JBCheckBox
import com.github.sukieva.gitcommitstats.stats.CommitStats
import com.github.sukieva.gitcommitstats.stats.CommitStatsCalculator
import com.github.sukieva.gitcommitstats.statusbar.CommitStatsWidget
import kotlinx.coroutines.*
import java.text.MessageFormat
import javax.swing.JComponent

class CommitStatsHandler(
    private val panel: CheckinProjectPanel,
    private val commitContext: CommitContext
) : CheckinHandler() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val calculator = CommitStatsCalculator()
    private val logger = thisLogger()

    private var computationJob: Job? = null
    private var currentStats: CommitStats = CommitStats()
    private var checkLargeCommit = true

    init {
        // Initial stats computation and show widget
        includedChangesChanged()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent {
        val checkBox = JBCheckBox(MyBundle.message("checkin.largeCommit.checkboxLabel"))
        checkBox.isSelected = checkLargeCommit

        return object : RefreshableOnComponent {
            override fun getComponent(): JComponent = checkBox

            override fun refresh() {
                // No need to refresh anything
            }

            override fun saveState() {
                checkLargeCommit = checkBox.isSelected
            }

            override fun restoreState() {
                checkBox.isSelected = checkLargeCommit
            }
        }
    }

    override fun includedChangesChanged() {
        // Debounce: cancel previous computation
        computationJob?.cancel()

        computationJob = scope.launch {
            try {
                delay(300) // Debounce 300ms

                val changes = panel.selectedChanges
                logger.debug("Computing stats for ${changes.size} changes")

                val stats = calculator.computeStats(changes)
                currentStats = stats

                withContext(Dispatchers.EDT) {
                    updateWidget(panel.project, stats, true)
                }
            } catch (e: CancellationException) {
                // Job was cancelled, ignore
                logger.debug("Stats computation cancelled")
            } catch (e: Exception) {
                logger.error("Failed to compute commit stats", e)
            }
        }
    }

    override fun beforeCheckin(): ReturnResult {
        // Only check if the option is enabled
        if (!checkLargeCommit) {
            return ReturnResult.COMMIT
        }

        val totalLines = currentStats.linesAdded + currentStats.linesDeleted

        if (totalLines > 500) {
            logger.info("Large commit detected: $totalLines lines")

            val message = MessageFormat.format(
                MyBundle.message("checkin.largeCommit.message"),
                totalLines
            )

            val result = Messages.showDialog(
                panel.project,
                message,
                MyBundle.message("checkin.largeCommit.title"),
                arrayOf(
                    MyBundle.message("checkin.largeCommit.proceed"),
                    MyBundle.message("checkin.largeCommit.cancel")
                ),
                1, // Default button index (Cancel)
                Messages.getWarningIcon()
            )

            // result == 0 means "Proceed Anyway" was clicked
            // result == 1 or -1 (dialog closed) means cancel
            return if (result == 0) {
                logger.info("User chose to proceed with large commit")
                ReturnResult.COMMIT
            } else {
                logger.info("User cancelled large commit")
                ReturnResult.CANCEL
            }
        }

        return ReturnResult.COMMIT
    }

    override fun checkinSuccessful() {
        // Hide widget and cleanup
        scope.launch(Dispatchers.EDT) {
            updateWidget(panel.project, CommitStats(), false)
        }
        scope.cancel()
    }

    override fun checkinFailed(exception: MutableList<VcsException>) {
        // Hide widget and cleanup
        scope.launch(Dispatchers.EDT) {
            updateWidget(panel.project, CommitStats(), false)
        }
        scope.cancel()
    }

    private fun updateWidget(project: Project, stats: CommitStats, visible: Boolean) {
        try {
            val statusBar = WindowManager.getInstance().getStatusBar(project)
            val widget = statusBar?.getWidget(CommitStatsWidget.ID) as? CommitStatsWidget
            widget?.updateStats(stats, visible)
        } catch (e: Exception) {
            logger.warn("Failed to update status bar widget", e)
        }
    }
}
