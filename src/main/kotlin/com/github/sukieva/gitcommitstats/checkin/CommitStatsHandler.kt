package com.github.sukieva.gitcommitstats.checkin

import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.wm.WindowManager
import com.github.sukieva.gitcommitstats.stats.CommitStats
import com.github.sukieva.gitcommitstats.stats.CommitStatsCalculator
import com.github.sukieva.gitcommitstats.statusbar.CommitStatsWidget
import kotlinx.coroutines.*

class CommitStatsHandler(
    private val panel: CheckinProjectPanel,
    private val commitContext: CommitContext
) : CheckinHandler() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val calculator = CommitStatsCalculator()
    private val logger = thisLogger()

    private var computationJob: Job? = null

    init {
        // Initial stats computation and show widget
        includedChangesChanged()
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
