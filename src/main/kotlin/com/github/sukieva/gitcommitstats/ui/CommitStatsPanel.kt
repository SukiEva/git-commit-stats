package com.github.sukieva.gitcommitstats.ui

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.github.sukieva.gitcommitstats.stats.CommitStats
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel

class CommitStatsPanel {
    val component: JPanel = JPanel(GridBagLayout())

    private val filesLabel = JBLabel("Files: -")
    private val linesLabel = JBLabel("Lines: -")

    init {
        component.border = JBUI.Borders.empty(8, 12)

        val gbc = GridBagConstraints()
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0

        gbc.gridx = 0
        gbc.gridy = 0
        component.add(filesLabel, gbc)

        gbc.gridy = 1
        component.add(linesLabel, gbc)
    }

    fun updateStats(stats: CommitStats) {
        filesLabel.text = formatFilesStats(stats)
        linesLabel.text = formatLinesStats(stats)
    }

    private fun formatFilesStats(stats: CommitStats): String {
        val parts = mutableListOf<String>()

        if (stats.filesModified > 0) {
            parts.add("${stats.filesModified} modified")
        }
        if (stats.filesAdded > 0) {
            parts.add("${stats.filesAdded} added")
        }
        if (stats.filesDeleted > 0) {
            parts.add("${stats.filesDeleted} deleted")
        }

        val binaryNote = if (stats.binaryFilesModified > 0) {
            " (${stats.binaryFilesModified} binary)"
        } else ""

        return if (parts.isEmpty()) {
            "Files: -"
        } else {
            "Files: ${parts.joinToString(", ")}$binaryNote"
        }
    }

    private fun formatLinesStats(stats: CommitStats): String {
        val total = stats.linesAdded + stats.linesDeleted

        return if (total == 0) {
            "Lines: -"
        } else {
            "Lines: +${stats.linesAdded}, -${stats.linesDeleted}"
        }
    }
}
