package com.github.sukieva.gitcommitstats.toolwindow.ui

import com.github.sukieva.gitcommitstats.MyBundle
import com.github.sukieva.gitcommitstats.toolwindow.model.FileHotspot
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.table.AbstractTableModel

class FileHotspotPanel(
    private val onFileDoubleClick: (filePath: String) -> Unit
) : JPanel() {

    private val tableModel = HotspotTableModel()
    private val table = JBTable(tableModel)
    private val scrollPane = JBScrollPane(table)

    init {
        layout = BorderLayout()
        add(scrollPane, BorderLayout.CENTER)

        // Configure table
        table.autoCreateRowSorter = true
        table.setShowGrid(true)

        // Add double-click listener
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val row = table.rowAtPoint(e.point)
                    if (row >= 0) {
                        val modelRow = table.convertRowIndexToModel(row)
                        val filePath = tableModel.getFilePathAt(modelRow)
                        onFileDoubleClick(filePath)
                    }
                }
            }
        })
    }

    fun updateHotspots(hotspots: List<FileHotspot>) {
        tableModel.updateHotspots(hotspots)
    }

    private class HotspotTableModel : AbstractTableModel() {

        private val columnNames = arrayOf(
            MyBundle.message("toolwindow.hotspot.column.rank"),
            MyBundle.message("toolwindow.hotspot.column.file"),
            MyBundle.message("toolwindow.hotspot.column.modifications"),
            MyBundle.message("toolwindow.hotspot.column.totalLines")
        )

        private var hotspots: List<FileHotspot> = emptyList()

        fun updateHotspots(newHotspots: List<FileHotspot>) {
            hotspots = newHotspots
            fireTableDataChanged()
        }

        fun getFilePathAt(rowIndex: Int): String {
            return hotspots[rowIndex].filePath
        }

        override fun getRowCount(): Int = hotspots.size

        override fun getColumnCount(): Int = columnNames.size

        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val hotspot = hotspots[rowIndex]
            return when (columnIndex) {
                0 -> rowIndex + 1 // Rank
                1 -> hotspot.fileName
                2 -> hotspot.modificationCount
                3 -> hotspot.totalLinesChanged
                else -> ""
            }
        }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                0, 2, 3 -> Integer::class.java
                else -> String::class.java
            }
        }
    }
}
