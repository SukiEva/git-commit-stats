package com.github.sukieva.gitcommitstats.checkin

import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.github.sukieva.gitcommitstats.stats.CommitStatsCalculator
import com.github.sukieva.gitcommitstats.ui.CommitStatsPanel
import kotlinx.coroutines.*
import javax.swing.JComponent

class CommitStatsHandler(
    private val panel: CheckinProjectPanel,
    private val commitContext: CommitContext
) : CheckinHandler() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val statsPanel = CommitStatsPanel()
    private val calculator = CommitStatsCalculator()
    private val logger = thisLogger()

    private var computationJob: Job? = null

    override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent {
        return object : RefreshableOnComponent {
            override fun getComponent(): JComponent = statsPanel.component

            override fun refresh() {
                // Called when panel is refreshed
                includedChangesChanged()
            }

            override fun saveState() {
                // No settings to persist
            }

            override fun restoreState() {
                // No settings to restore
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

                withContext(Dispatchers.EDT) {
                    statsPanel.updateStats(stats)
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
        // Cleanup
        scope.cancel()
    }

    override fun checkinFailed(exception: MutableList<VcsException>) {
        // Cleanup
        scope.cancel()
    }
}
