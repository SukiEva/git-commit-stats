package com.github.sukieva.gitcommitstats.toolwindow.service

import com.github.sukieva.gitcommitstats.stats.CommitStats
import com.github.sukieva.gitcommitstats.toolwindow.model.AuthorStats
import com.github.sukieva.gitcommitstats.toolwindow.model.CommitWithStats
import com.github.sukieva.gitcommitstats.toolwindow.model.DateRange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StatsAggregator {

    fun aggregate(commits: List<CommitWithStats>): AuthorStats {
        if (commits.isEmpty()) {
            return AuthorStats(
                author = "",
                timeRange = null,
                totalCommits = 0,
                aggregatedStats = CommitStats(),
                commits = emptyList(),
                dailyActivity = emptyMap()
            )
        }

        // Aggregate total stats
        val totalStats = commits
            .map { it.stats }
            .reduceOrNull { acc, stat -> acc + stat }
            ?: CommitStats()

        // Group by date for daily activity
        val dailyActivity = commits
            .groupBy {
                Instant.ofEpochMilli(it.date.time)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .mapValues { (_, dayCommits) ->
                dayCommits
                    .map { it.stats }
                    .reduce { acc, stat -> acc + stat }
            }

        // Calculate time range
        val timeRange = DateRange(
            start = commits.minOfOrNull { it.date } ?: commits.first().date,
            end = commits.maxOfOrNull { it.date } ?: commits.first().date
        )

        return AuthorStats(
            author = commits.first().author,
            timeRange = timeRange,
            totalCommits = commits.size,
            aggregatedStats = totalStats,
            commits = commits.sortedByDescending { it.date },
            dailyActivity = dailyActivity
        )
    }
}
