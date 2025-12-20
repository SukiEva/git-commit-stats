package com.github.sukieva.gitcommitstats.toolwindow.model

import com.github.sukieva.gitcommitstats.stats.CommitStats
import java.util.Date

data class CommitWithStats(
    val hash: String,
    val author: String,
    val authorEmail: String,
    val date: Date,
    val message: String,
    val stats: CommitStats
)
