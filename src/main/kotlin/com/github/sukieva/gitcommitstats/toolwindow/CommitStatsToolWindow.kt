package com.github.sukieva.gitcommitstats.toolwindow

import com.github.sukieva.gitcommitstats.MyBundle
import com.github.sukieva.gitcommitstats.toolwindow.model.CommitWithStats
import com.github.sukieva.gitcommitstats.toolwindow.service.AuthorService
import com.github.sukieva.gitcommitstats.toolwindow.service.CommitStatsAdapter
import com.github.sukieva.gitcommitstats.toolwindow.service.GitQueryService
import com.github.sukieva.gitcommitstats.toolwindow.service.StatsAggregator
import com.github.sukieva.gitcommitstats.toolwindow.service.VcsLogNavigationService
import com.github.sukieva.gitcommitstats.toolwindow.service.FileHotspotAnalyzer
import com.github.sukieva.gitcommitstats.toolwindow.ui.CommitListPanel
import com.github.sukieva.gitcommitstats.toolwindow.ui.FileHotspotPanel
import com.github.sukieva.gitcommitstats.toolwindow.ui.FilterPanel
import com.github.sukieva.gitcommitstats.toolwindow.ui.SummaryPanel
import com.intellij.ui.components.JBTabbedPane
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
    private val hotspotAnalyzer = FileHotspotAnalyzer()

    private val filterPanel = FilterPanel(::refreshData)
    private val summaryPanel = SummaryPanel()
    private val commitListPanel = CommitListPanel { hash -> onCommitDoubleClick(hash) }
    private val hotspotPanel = FileHotspotPanel { filePath -> onHotspotFileDoubleClick(filePath) }

    private val loadingLabel = JBLabel(MyBundle.message("toolwindow.loading"), SwingConstants.CENTER)
    private val contentPanel = JBPanel<JBPanel<*>>(BorderLayout())

    private var currentJob: Job? = null
    private var currentTabbedPane: JBTabbedPane? = null
    private var currentGitCommits: List<git4idea.GitCommit> = emptyList()
    private var currentCommitsWithStats: List<CommitWithStats> = emptyList()

    init {
        layout = BorderLayout()

        // Top: Filter panel (fixed, never removed)
        add(filterPanel, BorderLayout.NORTH)

        // Center: Content panel (will be updated with different views)
        contentPanel.add(loadingLabel, BorderLayout.CENTER)
        add(contentPanel, BorderLayout.CENTER)

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

                // Save current data for filtering
                currentGitCommits = commits
                currentCommitsWithStats = commitsWithStats

                // Aggregate stats
                val authorStats = aggregator.aggregate(commitsWithStats)

                // Analyze file hotspots
                val hotspots = hotspotAnalyzer.analyzeHotspots(commitsWithStats, commits, topN = 10)

                // Update UI
                withContext(Dispatchers.EDT) {
                    updateUI(authorStats, hotspots)
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
            contentPanel.removeAll()
            contentPanel.add(loadingLabel, BorderLayout.CENTER)
            contentPanel.revalidate()
            contentPanel.repaint()
        }
    }

    private fun showEmptyState() {
        contentPanel.removeAll()
        val emptyLabel = JBLabel(MyBundle.message("toolwindow.empty.noCommits"), SwingConstants.CENTER)
        contentPanel.add(emptyLabel, BorderLayout.CENTER)
        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun showError(message: String) {
        contentPanel.removeAll()
        val errorLabel = JBLabel("Error: $message", SwingConstants.CENTER)
        contentPanel.add(errorLabel, BorderLayout.CENTER)
        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun updateUI(
        stats: com.github.sukieva.gitcommitstats.toolwindow.model.AuthorStats,
        hotspots: List<com.github.sukieva.gitcommitstats.toolwindow.model.FileHotspot>
    ) {
        // Update panels
        summaryPanel.updateStats(stats)
        commitListPanel.updateCommits(stats.commits)
        hotspotPanel.updateHotspots(hotspots)

        // Build content layout
        contentPanel.removeAll()

        // Create tabbed pane
        val tabbedPane = JBTabbedPane()
        currentTabbedPane = tabbedPane

        // Commits tab
        val commitsTab = JBPanel<JBPanel<*>>(BorderLayout())
        commitsTab.add(summaryPanel, BorderLayout.NORTH)
        commitsTab.add(commitListPanel, BorderLayout.CENTER)
        tabbedPane.addTab(MyBundle.message("toolwindow.tab.commits"), commitsTab)

        // Hotspots tab
        tabbedPane.addTab(MyBundle.message("toolwindow.tab.hotspots"), hotspotPanel)

        contentPanel.add(tabbedPane, BorderLayout.CENTER)

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun onCommitDoubleClick(hash: String) {
        scope.launch {
            vcsLogNavService.navigateToCommit(hash)
        }
    }

    private fun onHotspotFileDoubleClick(filePath: String) {
        logger.info("Double-clicked on hotspot file: $filePath")

        // Filter commits that modified this file
        val filteredCommits = currentCommitsWithStats.filter { commitWithStats ->
            val gitCommit = currentGitCommits.find { it.id.asString() == commitWithStats.hash }
            gitCommit?.changes?.any { change ->
                val changedFilePath = change.afterRevision?.file?.path ?: change.beforeRevision?.file?.path
                changedFilePath == filePath
            } ?: false
        }

        logger.info("Found ${filteredCommits.size} commits that modified $filePath")

        if (filteredCommits.isEmpty()) {
            logger.warn("No commits found for file: $filePath")
            return
        }

        // Update commit list with filtered commits
        commitListPanel.updateCommits(filteredCommits)

        // Update summary with filtered commits stats
        val filteredStats = aggregator.aggregate(filteredCommits)
        summaryPanel.updateStats(filteredStats)

        // Switch to Commits tab
        currentTabbedPane?.selectedIndex = 0
    }

    override fun dispose() {
        scope.cancel()
    }
}
