package com.github.sukieva.gitcommitstats.toolwindow.ui

import com.github.sukieva.gitcommitstats.stats.CommitStats
import com.github.sukieva.gitcommitstats.toolwindow.model.CommitWithStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class CommitStatsTableModelTest {

    @Test
    fun `files column uses numeric values`() {
        val model = CommitListPanel.CommitStatsTableModel()
        val commit = CommitWithStats(
            hash = "abc123",
            author = "Author",
            authorEmail = "author@example.com",
            date = Date(0),
            message = "Test commit message",
            stats = CommitStats(filesModified = 1, filesAdded = 2, filesDeleted = 0)
        )

        model.updateCommits(listOf(commit))

        val value = model.getValueAt(0, 5)

        assertTrue(value is Int)
        assertEquals(3, value)
        assertEquals(Integer::class.java, model.getColumnClass(5))
    }
}
