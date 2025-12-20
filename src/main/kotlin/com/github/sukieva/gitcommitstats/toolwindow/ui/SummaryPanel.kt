package com.github.sukieva.gitcommitstats.toolwindow.ui

import com.github.sukieva.gitcommitstats.MyBundle
import com.github.sukieva.gitcommitstats.toolwindow.model.AuthorStats
import com.intellij.ui.JBColor
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class SummaryPanel : JPanel() {

    private val totalCommitsLabel = createValueLabel()
    private val filesChangedLabel = createValueLabel()
    private val filesAddedLabel = createValueLabel()
    private val filesDeletedLabel = createValueLabel()
    private val linesAddedLabel = createValueLabel(JBColor.GREEN)
    private val linesDeletedLabel = createValueLabel(JBColor.RED)

    init {
        layout = GridLayout(2, 3, 10, 10)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        add(createStatBox(MyBundle.message("toolwindow.summary.totalCommits"), totalCommitsLabel))
        add(createStatBox(MyBundle.message("toolwindow.summary.filesChanged"), filesChangedLabel))
        add(createStatBox(MyBundle.message("toolwindow.summary.filesAdded"), filesAddedLabel))
        add(createStatBox(MyBundle.message("toolwindow.summary.filesDeleted"), filesDeletedLabel))
        add(createStatBox(MyBundle.message("toolwindow.summary.linesAdded"), linesAddedLabel))
        add(createStatBox(MyBundle.message("toolwindow.summary.linesDeleted"), linesDeletedLabel))
    }

    private fun createStatBox(label: String, valueLabel: JLabel): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.GRAY),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        )

        val titleLabel = JLabel(label, SwingConstants.CENTER)
        titleLabel.font = titleLabel.font.deriveFont(Font.PLAIN, 11f)

        panel.add(titleLabel, BorderLayout.NORTH)
        panel.add(valueLabel, BorderLayout.CENTER)

        return panel
    }

    private fun createValueLabel(color: java.awt.Color = JBColor.foreground()): JLabel {
        val label = JLabel("0", SwingConstants.CENTER)
        label.font = label.font.deriveFont(Font.BOLD, 24f)
        label.foreground = color
        return label
    }

    fun updateStats(stats: AuthorStats?) {
        if (stats == null) {
            totalCommitsLabel.text = "0"
            filesChangedLabel.text = "0"
            filesAddedLabel.text = "0"
            filesDeletedLabel.text = "0"
            linesAddedLabel.text = "0"
            linesDeletedLabel.text = "0"
            return
        }

        totalCommitsLabel.text = stats.totalCommits.toString()
        filesChangedLabel.text = (
            stats.aggregatedStats.filesModified +
            stats.aggregatedStats.filesAdded +
            stats.aggregatedStats.filesDeleted
        ).toString()
        filesAddedLabel.text = stats.aggregatedStats.filesAdded.toString()
        filesDeletedLabel.text = stats.aggregatedStats.filesDeleted.toString()
        linesAddedLabel.text = stats.aggregatedStats.linesAdded.toString()
        linesDeletedLabel.text = stats.aggregatedStats.linesDeleted.toString()
    }
}
