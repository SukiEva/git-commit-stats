package com.github.sukieva.gitcommitstats.toolwindow.model

import com.github.sukieva.gitcommitstats.stats.CommitStats
import java.time.LocalDate

data class AuthorStats(
    val author: String,
    val timeRange: DateRange?,
    val totalCommits: Int,
    val aggregatedStats: CommitStats,
    val commits: List<CommitWithStats>,
    val dailyActivity: Map<LocalDate, CommitStats>
)
