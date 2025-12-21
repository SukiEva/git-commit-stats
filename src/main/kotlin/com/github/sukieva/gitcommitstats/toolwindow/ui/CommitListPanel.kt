package com.github.sukieva.gitcommitstats.toolwindow.ui

import com.github.sukieva.gitcommitstats.MyBundle
import com.github.sukieva.gitcommitstats.toolwindow.model.CommitWithStats
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.text.SimpleDateFormat
import javax.swing.JPanel
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableRowSorter

class CommitListPanel(
    private val onCommitDoubleClick: ((String) -> Unit)? = null
) : JPanel() {

    private val tableModel = CommitStatsTableModel()
    private val table = JBTable(tableModel)
    private val scrollPane = JBScrollPane(table)

    init {
        layout = BorderLayout()
        add(scrollPane, BorderLayout.CENTER)

        // Enable sorting
        table.rowSorter = TableRowSorter(tableModel)
        table.autoCreateRowSorter = true

        // Add double-click listener
        table.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2 && javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                    val viewRow = table.rowAtPoint(e.point)
                    if (viewRow != -1) {
                        val modelRow = table.convertRowIndexToModel(viewRow)
                        val commitHash = tableModel.getCommitHashAt(modelRow)
                        commitHash?.let { onCommitDoubleClick?.invoke(it) }
                    }
                }
            }
        })
    }

    fun updateCommits(commits: List<CommitWithStats>) {
        tableModel.updateCommits(commits)
    }

    private class CommitStatsTableModel : AbstractTableModel() {

        private val columnNames = arrayOf(
            MyBundle.message("toolwindow.commits.column.hash"),
            MyBundle.message("toolwindow.commits.column.date"),
            MyBundle.message("toolwindow.commits.column.message"),
            MyBundle.message("toolwindow.commits.column.files"),
            MyBundle.message("toolwindow.commits.column.lines")
        )

        private var commits: List<CommitWithStats> = emptyList()
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm")

        fun updateCommits(newCommits: List<CommitWithStats>) {
            commits = newCommits
            fireTableDataChanged()
        }

        override fun getRowCount(): Int = commits.size

        override fun getColumnCount(): Int = columnNames.size

        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val commit = commits[rowIndex]
            return when (columnIndex) {
                0 -> commit.hash.take(8)
                1 -> dateFormat.format(commit.date)
                2 -> {
                    val message = commit.message.replace("\n", " ").trim()
                    if (message.length > 60) message.take(57) + "..." else message
                }
                3 -> {
                    val total = commit.stats.filesModified + commit.stats.filesAdded + commit.stats.filesDeleted
                    total.toString()
                }
                4 -> "+${commit.stats.linesAdded}/-${commit.stats.linesDeleted}"
                else -> ""
            }
        }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                3 -> Integer::class.java
                else -> String::class.java
            }
        }

        fun getCommitHashAt(rowIndex: Int): String? {
            return if (rowIndex in commits.indices) {
                commits[rowIndex].hash
            } else {
                null
            }
        }
    }
}
