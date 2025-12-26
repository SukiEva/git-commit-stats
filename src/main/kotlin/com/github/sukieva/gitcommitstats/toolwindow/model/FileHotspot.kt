package com.github.sukieva.gitcommitstats.toolwindow.model

data class FileHotspot(
    val filePath: String,
    val modificationCount: Int,
    val totalLinesChanged: Int
) {
    val fileName: String
        get() = filePath.substringAfterLast('/')
}
