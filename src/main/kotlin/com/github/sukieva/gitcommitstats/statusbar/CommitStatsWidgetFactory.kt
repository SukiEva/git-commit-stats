package com.github.sukieva.gitcommitstats.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class CommitStatsWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "CommitStatsWidget"

    override fun getDisplayName(): String = "Commit Statistics"

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget {
        return CommitStatsWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        // Cleanup handled by widget itself
    }

    override fun canBeEnabledOn(statusBar: com.intellij.openapi.wm.StatusBar): Boolean = true
}
