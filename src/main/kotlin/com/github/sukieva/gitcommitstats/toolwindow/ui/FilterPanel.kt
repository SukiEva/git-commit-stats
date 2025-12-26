package com.github.sukieva.gitcommitstats.toolwindow.ui

import com.github.sukieva.gitcommitstats.MyBundle
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import javax.swing.JButton
import javax.swing.JFormattedTextField
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.text.JTextComponent

class FilterPanel(
    private val onFilterChanged: (author: String?, startDate: Date?, endDate: Date?) -> Unit
) : JPanel() {

    private val logger = thisLogger()
    private var allAuthors: List<String> = emptyList()
    private var isUpdating = false

    private val authorComboBox = ComboBox<String>().apply {
        isEditable = true  // Allow manual input
        addItem("")  // Add initial empty item
    }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

    private val startDateField = JFormattedTextField(dateFormat).apply {
        // Default to today (beginning of day)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        value = calendar.time
        columns = 10
    }

    private val endDateField = JFormattedTextField(dateFormat).apply {
        // Default to today
        value = Date()
        columns = 10
    }

    private val refreshButton = JButton(MyBundle.message("toolwindow.filter.refresh"))
    private val autoRefreshCheckbox = JBCheckBox(MyBundle.message("toolwindow.filter.autoRefresh"), true)

    init {
        layout = FlowLayout(FlowLayout.LEFT, 10, 5)

        // Author section
        add(JLabel(MyBundle.message("toolwindow.filter.author")))
        add(authorComboBox)

        // Date Range section with quick buttons first
        add(JLabel(MyBundle.message("toolwindow.filter.dateRange")))

        // Quick range buttons
        val todayButton = JButton(MyBundle.message("toolwindow.filter.today"))
        todayButton.addActionListener { setQuickRange(0) }
        add(todayButton)

        val last7DaysButton = JButton(MyBundle.message("toolwindow.filter.last7days"))
        last7DaysButton.addActionListener { setQuickRange(7) }
        add(last7DaysButton)

        val last30DaysButton = JButton(MyBundle.message("toolwindow.filter.last30days"))
        last30DaysButton.addActionListener { setQuickRange(30) }
        add(last30DaysButton)

        val last90DaysButton = JButton(MyBundle.message("toolwindow.filter.last90days"))
        last90DaysButton.addActionListener { setQuickRange(90) }
        add(last90DaysButton)

        // Date fields
        add(startDateField)
        add(JLabel(MyBundle.message("toolwindow.filter.to")))
        add(endDateField)

        // Action buttons
        add(refreshButton)
        add(autoRefreshCheckbox)

        // Add event listeners
        authorComboBox.addItemListener { e ->
            // Only trigger when not updating programmatically and item is actually selected (not during typing)
            if (e.stateChange == ItemEvent.SELECTED && !isUpdating && autoRefreshCheckbox.isSelected) {
                triggerFilterChange()
            }
        }

        refreshButton.addActionListener {
            triggerFilterChange()
        }

        // Add autocomplete support for author input
        setupAutocomplete()
    }

    private fun setupAutocomplete() {
        val editor = authorComboBox.editor.editorComponent as? JTextComponent ?: return

        editor.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                // Trigger refresh when user presses Enter
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    if (autoRefreshCheckbox.isSelected) {
                        triggerFilterChange()
                    }
                }
            }

            override fun keyReleased(e: KeyEvent) {
                // Skip special keys
                if (e.keyCode == KeyEvent.VK_ENTER || e.keyCode == KeyEvent.VK_ESCAPE ||
                    e.keyCode == KeyEvent.VK_UP || e.keyCode == KeyEvent.VK_DOWN) {
                    return
                }

                if (isUpdating) return

                SwingUtilities.invokeLater {
                    val text = editor.text ?: ""
                    if (text.isEmpty()) {
                        updateComboBoxItems(allAuthors)
                        return@invokeLater
                    }

                    // Filter authors that match the input and update dropdown
                    val matches = allAuthors.filter {
                        it.contains(text, ignoreCase = true)
                    }

                    updateComboBoxItems(matches)

                    // Show dropdown if there are matches
                    if (matches.isNotEmpty() && !authorComboBox.isPopupVisible) {
                        authorComboBox.showPopup()
                    }
                }
            }
        })
    }

    private fun updateComboBoxItems(items: List<String>) {
        isUpdating = true
        try {
            val currentText = (authorComboBox.editor.editorComponent as? JTextComponent)?.text ?: ""

            authorComboBox.removeAllItems()
            authorComboBox.addItem("")  // Empty option for "All authors"
            items.forEach { authorComboBox.addItem(it) }

            // Restore the text in the editor
            (authorComboBox.editor.editorComponent as? JTextComponent)?.text = currentText
        } finally {
            isUpdating = false
        }
    }

    private fun setQuickRange(daysAgo: Int) {
        val endDate = Date()
        val calendar = Calendar.getInstance()
        calendar.time = endDate

        if (daysAgo == 0) {
            // Today: set start to beginning of today
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
        } else {
            // Last N days
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        }

        startDateField.value = calendar.time
        endDateField.value = endDate

        if (autoRefreshCheckbox.isSelected) {
            triggerFilterChange()
        }
    }

    fun setAuthors(authors: List<String>) {
        logger.info("Loaded ${authors.size} authors for filter dropdown")
        allAuthors = authors

        // Preserve current selection
        val currentSelection = authorComboBox.selectedItem

        isUpdating = true
        try {
            authorComboBox.removeAllItems()
            authorComboBox.addItem("")  // Empty option for "All authors"
            authors.forEach { authorComboBox.addItem(it) }

            // Restore selection if it was valid
            if (currentSelection != null && (currentSelection in authors || currentSelection == "")) {
                authorComboBox.selectedItem = currentSelection
            }

            // Force UI refresh
            authorComboBox.revalidate()
            authorComboBox.repaint()
        } finally {
            isUpdating = false
        }
    }

    private fun triggerFilterChange() {
        // Get author from editable combo box - handle both selection and manual input
        val author = when (val item = authorComboBox.selectedItem) {
            is String -> item.trim()
            else -> authorComboBox.editor.item?.toString()?.trim() ?: ""
        }

        val startDate = try {
            startDateField.value as? Date
        } catch (e: Exception) {
            null
        }
        val endDate = try {
            endDateField.value as? Date
        } catch (e: Exception) {
            null
        }

        onFilterChanged(
            if (author.isEmpty()) null else author,
            startDate,
            endDate
        )
    }

    fun triggerInitialLoad() {
        triggerFilterChange()
    }
}
