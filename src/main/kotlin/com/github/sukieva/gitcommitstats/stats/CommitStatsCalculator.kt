package com.github.sukieva.gitcommitstats.stats

import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vcs.changes.Change
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

data class CommitStats(
    val filesModified: Int = 0,
    val filesAdded: Int = 0,
    val filesDeleted: Int = 0,
    val linesAdded: Int = 0,
    val linesDeleted: Int = 0,
    val binaryFilesModified: Int = 0
) {
    operator fun plus(other: CommitStats) = CommitStats(
        filesModified = filesModified + other.filesModified,
        filesAdded = filesAdded + other.filesAdded,
        filesDeleted = filesDeleted + other.filesDeleted,
        linesAdded = linesAdded + other.linesAdded,
        linesDeleted = linesDeleted + other.linesDeleted,
        binaryFilesModified = binaryFilesModified + other.binaryFilesModified
    )
}

class CommitStatsCalculator {
    private val comparisonManager = ComparisonManager.getInstance()
    private val logger = thisLogger()

    suspend fun computeStats(changes: Collection<Change>): CommitStats {
        if (changes.isEmpty()) {
            return CommitStats()
        }

        return changes.map { change ->
            coroutineContext.ensureActive() // Support cancellation
            computeChangeStats(change)
        }.reduce { acc, stats -> acc + stats }
    }

    private fun computeChangeStats(change: Change): CommitStats {
        val beforeRevision = change.beforeRevision
        val afterRevision = change.afterRevision

        return when {
            // New file
            beforeRevision == null && afterRevision != null -> {
                if (isBinaryFile(afterRevision.file.path)) {
                    CommitStats(filesAdded = 1)
                } else {
                    try {
                        val content = afterRevision.content ?: ""
                        val lines = countLines(content)
                        CommitStats(filesAdded = 1, linesAdded = lines)
                    } catch (e: Exception) {
                        logger.warn("Failed to get content for new file ${afterRevision.file.path}", e)
                        CommitStats(filesAdded = 1)
                    }
                }
            }

            // Deleted file
            beforeRevision != null && afterRevision == null -> {
                if (isBinaryFile(beforeRevision.file.path)) {
                    CommitStats(filesDeleted = 1)
                } else {
                    try {
                        val content = beforeRevision.content ?: ""
                        val lines = countLines(content)
                        CommitStats(filesDeleted = 1, linesDeleted = lines)
                    } catch (e: Exception) {
                        logger.warn("Failed to get content for deleted file ${beforeRevision.file.path}", e)
                        CommitStats(filesDeleted = 1)
                    }
                }
            }

            // Modified file
            beforeRevision != null && afterRevision != null -> {
                if (isBinaryFile(afterRevision.file.path)) {
                    CommitStats(filesModified = 1, binaryFilesModified = 1)
                } else {
                    try {
                        val beforeText = beforeRevision.content ?: ""
                        val afterText = afterRevision.content ?: ""
                        computeTextDiff(beforeText, afterText)
                    } catch (e: Exception) {
                        logger.warn("Failed to compute diff for ${afterRevision.file.path}", e)
                        CommitStats(filesModified = 1)
                    }
                }
            }

            else -> CommitStats()
        }
    }

    private fun computeTextDiff(beforeText: String, afterText: String): CommitStats {
        val indicator = ProgressManager.getInstance().progressIndicator

        return try {
            val fragments = comparisonManager.compareLines(
                beforeText,
                afterText,
                ComparisonPolicy.DEFAULT,
                indicator
            )

            var additions = 0
            var deletions = 0

            for (fragment in fragments) {
                val leftLines = fragment.endLine1 - fragment.startLine1
                val rightLines = fragment.endLine2 - fragment.startLine2

                deletions += leftLines
                additions += rightLines
            }

            CommitStats(
                filesModified = 1,
                linesAdded = additions,
                linesDeleted = deletions
            )
        } catch (e: Exception) {
            // Diff too large or computation error
            logger.info("Diff computation failed, showing file count only", e)
            CommitStats(filesModified = 1)
        }
    }

    private fun countLines(content: String): Int {
        if (content.isEmpty()) return 0
        return content.count { it == '\n' } + if (content.last() != '\n') 1 else 0
    }

    private fun isBinaryFile(path: String): Boolean {
        val binaryExtensions = setOf(
            "png", "jpg", "jpeg", "gif", "bmp", "ico", "svg",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "zip", "tar", "gz", "rar", "7z",
            "exe", "dll", "so", "dylib",
            "class", "jar", "war", "ear",
            "mp3", "mp4", "avi", "mov", "mkv",
            "ttf", "otf", "woff", "woff2"
        )
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in binaryExtensions
    }
}
