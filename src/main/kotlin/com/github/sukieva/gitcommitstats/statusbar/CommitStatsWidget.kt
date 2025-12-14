package com.github.sukieva.gitcommitstats.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.util.Consumer
import com.github.sukieva.gitcommitstats.stats.CommitStats
import java.awt.event.MouseEvent

class CommitStatsWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.TextPresentation {

    private var currentStats: CommitStats = CommitStats()
    private var isVisible: Boolean = false

    companion object {
        const val ID = "CommitStatsWidget"
    }

    override fun ID(): String = ID

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    fun updateStats(stats: CommitStats, visible: Boolean) {
        currentStats = stats
        isVisible = visible
        myStatusBar?.updateWidget(ID())
    }

    override fun getText(): String {
        if (!isVisible) return ""

        val fileCount = currentStats.filesModified + currentStats.filesAdded + currentStats.filesDeleted
        if (fileCount == 0) return ""

        val parts = mutableListOf<String>()

        // File count
        parts.add("$fileCount file${if (fileCount > 1) "s" else ""}")

        // Line changes
        val totalLines = currentStats.linesAdded + currentStats.linesDeleted
        if (totalLines > 0) {
            parts.add("+${currentStats.linesAdded}/-${currentStats.linesDeleted}")
        }

        return parts.joinToString(", ")
    }

    override fun getTooltipText(): String? {
        if (!isVisible || currentStats.filesModified + currentStats.filesAdded + currentStats.filesDeleted == 0) {
            return null
        }

        val lines = mutableListOf<String>()

        if (currentStats.filesModified > 0) {
            lines.add("${currentStats.filesModified} file${if (currentStats.filesModified > 1) "s" else ""} modified")
        }
        if (currentStats.filesAdded > 0) {
            lines.add("${currentStats.filesAdded} file${if (currentStats.filesAdded > 1) "s" else ""} added")
        }
        if (currentStats.filesDeleted > 0) {
            lines.add("${currentStats.filesDeleted} file${if (currentStats.filesDeleted > 1) "s" else ""} deleted")
        }
        if (currentStats.binaryFilesModified > 0) {
            lines.add("${currentStats.binaryFilesModified} binary file${if (currentStats.binaryFilesModified > 1) "s" else ""}")
        }

        val totalLines = currentStats.linesAdded + currentStats.linesDeleted
        if (totalLines > 0) {
            lines.add("+${currentStats.linesAdded} lines added")
            lines.add("-${currentStats.linesDeleted} lines deleted")
        }

        return lines.joinToString("\n")
    }

    override fun getAlignment(): Float = 0.5f

    override fun getClickConsumer(): Consumer<MouseEvent>? = null
}
