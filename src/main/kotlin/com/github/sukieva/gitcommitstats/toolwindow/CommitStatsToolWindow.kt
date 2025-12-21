package com.github.sukieva.gitcommitstats.toolwindow

import com.github.sukieva.gitcommitstats.MyBundle
import com.github.sukieva.gitcommitstats.toolwindow.model.CommitWithStats
import com.github.sukieva.gitcommitstats.toolwindow.service.AuthorService
import com.github.sukieva.gitcommitstats.toolwindow.service.CommitStatsAdapter
import com.github.sukieva.gitcommitstats.toolwindow.service.GitQueryService
import com.github.sukieva.gitcommitstats.toolwindow.service.StatsAggregator
import com.github.sukieva.gitcommitstats.toolwindow.service.VcsLogNavigationService
import com.github.sukieva.gitcommitstats.toolwindow.ui.CommitListPanel
import com.github.sukieva.gitcommitstats.toolwindow.ui.FilterPanel
import com.github.sukieva.gitcommitstats.toolwindow.ui.SummaryPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import kotlinx.coroutines.*
import java.awt.BorderLayout
import java.util.Date
import javax.swing.SwingConstants

class CommitStatsToolWindow(private val project: Project) : JBPanel<JBPanel<*>>(BorderLayout()), Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val logger = thisLogger()

    private val gitQueryService = project.service<GitQueryService>()
    private val authorService = project.service<AuthorService>()
    private val vcsLogNavService = project.service<VcsLogNavigationService>()
    private val adapter = CommitStatsAdapter(project)
    private val aggregator = StatsAggregator()

    private val filterPanel = FilterPanel(::refreshData)
    private val summaryPanel = SummaryPanel()
    private val commitListPanel = CommitListPanel { hash -> onCommitDoubleClick(hash) }

    private val loadingLabel = JBLabel(MyBundle.message("toolwindow.loading"), SwingConstants.CENTER)

    private var currentJob: Job? = null

    init {
        layout = BorderLayout()

        // Top: Filter panel
        add(filterPanel, BorderLayout.NORTH)

        // Center: Loading label (will be replaced with content)
        add(loadingLabel, BorderLayout.CENTER)

        // Load authors and trigger initial data load
        scope.launch {
            val authors = authorService.getAuthors()
            withContext(Dispatchers.EDT) {
                filterPanel.setAuthors(authors)
                filterPanel.triggerInitialLoad()
            }
        }
    }

    private fun refreshData(author: String?, startDate: Date?, endDate: Date?) {
        currentJob?.cancel()

        currentJob = scope.launch {
            try {
                delay(300) // Debounce

                // Show loading state
                withContext(Dispatchers.EDT) {
                    showLoading(true)
                }

                // Fetch commits for the selected date range
                logger.info("Fetching commits for author=$author, range=[$startDate, $endDate]")
                val commits = gitQueryService.fetchCommits(author, startDate, endDate)

                if (commits.isEmpty()) {
                    withContext(Dispatchers.EDT) {
                        showEmptyState()
                    }
                    return@launch
                }

                // Compute stats for commits in parallel
                val commitsWithStats = commits.map { commit ->
                    async(Dispatchers.Default) {
                        try {
                            val stats = adapter.computeStatsForCommit(commit)
                            CommitWithStats(
                                hash = commit.id.asString(),
                                author = commit.author.name,
                                authorEmail = commit.author.email,
                                date = Date(commit.commitTime),
                                message = commit.subject,
                                stats = stats
                            )
                        } catch (e: Exception) {
                            logger.warn("Failed to compute stats for commit ${commit.id.toShortString()}", e)
                            null
                        }
                    }
                }.awaitAll().filterNotNull()

                // Aggregate stats
                val authorStats = aggregator.aggregate(commitsWithStats)

                // Update UI
                withContext(Dispatchers.EDT) {
                    updateUI(authorStats)
                    showLoading(false)
                }

            } catch (e: CancellationException) {
                logger.debug("Stats computation cancelled")
            } catch (e: Exception) {
                logger.error("Failed to fetch commit stats", e)
                withContext(Dispatchers.EDT) {
                    showError(e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        if (loading) {
            removeAll()
            add(filterPanel, BorderLayout.NORTH)
            add(loadingLabel, BorderLayout.CENTER)
            revalidate()
            repaint()
        }
    }

    private fun showEmptyState() {
        removeAll()
        add(filterPanel, BorderLayout.NORTH)
        val emptyLabel = JBLabel(MyBundle.message("toolwindow.empty.noCommits"), SwingConstants.CENTER)
        add(emptyLabel, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    private fun showError(message: String) {
        removeAll()
        add(filterPanel, BorderLayout.NORTH)
        val errorLabel = JBLabel("Error: $message", SwingConstants.CENTER)
        add(errorLabel, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    private fun updateUI(stats: com.github.sukieva.gitcommitstats.toolwindow.model.AuthorStats) {
        // Update panels
        summaryPanel.updateStats(stats)
        commitListPanel.updateCommits(stats.commits)

        // Build content layout
        removeAll()
        add(filterPanel, BorderLayout.NORTH)

        val contentPanel = JBPanel<JBPanel<*>>(BorderLayout())
        contentPanel.add(summaryPanel, BorderLayout.NORTH)
        contentPanel.add(commitListPanel, BorderLayout.CENTER)

        add(contentPanel, BorderLayout.CENTER)

        revalidate()
        repaint()
    }

    private fun onCommitDoubleClick(hash: String) {
        scope.launch {
            vcsLogNavService.navigateToCommit(hash)
        }
    }

    override fun dispose() {
        scope.cancel()
    }
}
