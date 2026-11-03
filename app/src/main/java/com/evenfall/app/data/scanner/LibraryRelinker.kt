package com.evenfall.app.data.scanner

import com.evenfall.app.data.db.entity.BookEntity
import kotlin.math.abs

data class BookFileDescriptor(
    val name: String,
    val sizeBytes: Long
)

data class OldBookMatchInfo(
    val id: Long,
    val title: String,
    val author: String,
    val totalDurationMs: Long,
    val relativePath: String,
    val fileCount: Int,
    val files: List<BookFileDescriptor> = emptyList(),
    val bookEntity: BookEntity
)

data class NewBookMatchInfo(
    val identifier: String,
    val title: String,
    val author: String,
    val totalDurationMs: Long,
    val relativePath: String,
    val fileCount: Int,
    val files: List<BookFileDescriptor> = emptyList(),
    val preview: ScannedBookPreview? = null
)

enum class MatchConfidence {
    MATCHED,    // High-confidence, unambiguous match
    AMBIGUOUS,  // Multiple candidates or partial match requiring user review
    UNMATCHED   // Low match score
}

data class ScoredCandidate(
    val newBook: NewBookMatchInfo,
    val score: Int,
    val pathScore: Int,
    val metadataScore: Int,
    val fileScore: Int
)

data class RelinkMatchResult(
    val oldBook: OldBookMatchInfo,
    val status: MatchConfidence,
    val matchedCandidate: NewBookMatchInfo? = null,
    val candidates: List<ScoredCandidate> = emptyList()
)

data class RelinkAnalysis(
    val matched: List<RelinkMatchResult>,
    val ambiguous: List<RelinkMatchResult>,
    val unmatchedOld: List<RelinkMatchResult>,
    val unmatchedNew: List<NewBookMatchInfo>
)

object LibraryRelinker {

    /**
     * Scores match similarity between old unavailable book and new book candidate:
     * 1. Relative path (up to 70 pts)
     * 2. Title + Author + total duration (up to 70 pts)
     * 3. File names and sizes (up to 70 pts)
     */
    fun computeMatchScore(old: OldBookMatchInfo, new: NewBookMatchInfo): ScoredCandidate {
        var pathScore = 0
        var metadataScore = 0
        var fileScore = 0

        // 1. Relative path match
        val oldPathClean = sanitizePath(old.relativePath)
        val newPathClean = sanitizePath(new.relativePath)
        if (oldPathClean.isNotEmpty() && newPathClean.isNotEmpty()) {
            if (oldPathClean.equals(newPathClean, ignoreCase = true)) {
                pathScore += 70
            } else {
                val oldFolder = oldPathClean.substringAfterLast('/')
                val newFolder = newPathClean.substringAfterLast('/')
                if (oldFolder.equals(newFolder, ignoreCase = true)) {
                    pathScore += 40
                }
            }
        }

        // 2. Title + Author + total duration
        val normOldTitle = normalizeText(old.title)
        val normNewTitle = normalizeText(new.title)
        val titleMatch = normOldTitle.isNotEmpty() && (normOldTitle == normNewTitle || normOldTitle.contains(normNewTitle) || normNewTitle.contains(normOldTitle))

        val normOldAuthor = normalizeText(old.author)
        val normNewAuthor = normalizeText(new.author)
        val authorMatch = normOldAuthor.isNotEmpty() && normOldAuthor != "unknownauthor" && (normOldAuthor == normNewAuthor || normOldAuthor.contains(normNewAuthor) || normNewAuthor.contains(normOldAuthor))

        val durationDiff = abs(old.totalDurationMs - new.totalDurationMs)
        val durationMatch = old.totalDurationMs > 0 && new.totalDurationMs > 0 && durationDiff <= 5000L

        if (titleMatch && authorMatch && durationMatch) {
            metadataScore += 70
        } else {
            if (titleMatch) metadataScore += 30
            if (authorMatch) metadataScore += 20
            if (durationMatch) metadataScore += 20
            else if (old.totalDurationMs > 0 && durationDiff <= 30000L) metadataScore += 10
        }

        // 3. File names and sizes
        if (old.files.isNotEmpty() && new.files.isNotEmpty()) {
            val oldNames = old.files.map { it.name.lowercase().trim() }.toSet()
            val newNames = new.files.map { it.name.lowercase().trim() }.toSet()
            val commonNames = oldNames.intersect(newNames)
            val nameRatio = commonNames.size.toDouble() / oldNames.size.toDouble().coerceAtLeast(1.0)
            if (nameRatio >= 0.9) {
                fileScore += 40
            } else if (nameRatio >= 0.5) {
                fileScore += 20
            }

            // Size match
            val oldSizes = old.files.associate { it.name.lowercase().trim() to it.sizeBytes }
            val matchedSizesCount = new.files.count {
                val expectedSize = oldSizes[it.name.lowercase().trim()]
                expectedSize != null && expectedSize > 0 && expectedSize == it.sizeBytes
            }
            val sizeRatio = matchedSizesCount.toDouble() / old.files.size.toDouble().coerceAtLeast(1.0)
            if (sizeRatio >= 0.8) {
                fileScore += 30
            } else if (sizeRatio >= 0.4) {
                fileScore += 15
            }
        } else if (old.fileCount > 0 && old.fileCount == new.fileCount) {
            fileScore += 15
        }

        val totalScore = (pathScore + metadataScore + fileScore).coerceAtMost(100)
        return ScoredCandidate(new, totalScore, pathScore, metadataScore, fileScore)
    }

    /**
     * Matches old books against newly scanned candidates and partitions into
     * Matched (score >= 70), Ambiguous (30..69 or close scores), and Unmatched.
     */
    fun matchBooks(
        oldBooks: List<OldBookMatchInfo>,
        newBooks: List<NewBookMatchInfo>
    ): RelinkAnalysis {
        val matchedList = mutableListOf<RelinkMatchResult>()
        val ambiguousList = mutableListOf<RelinkMatchResult>()
        val unmatchedOldList = mutableListOf<RelinkMatchResult>()
        val claimedNewIdentifiers = mutableSetOf<String>()

        for (old in oldBooks) {
            val candidateScores = newBooks
                .map { computeMatchScore(old, it) }
                .filter { it.score >= 30 }
                .sortedByDescending { it.score }

            if (candidateScores.isEmpty()) {
                unmatchedOldList.add(
                    RelinkMatchResult(old, MatchConfidence.UNMATCHED, null, emptyList())
                )
            } else {
                val best = candidateScores.first()
                val secondBest = candidateScores.getOrNull(1)

                val isAmbiguous = (secondBest != null && (best.score - secondBest.score) < 15 && secondBest.score >= 50) || best.score < 70

                if (isAmbiguous) {
                    ambiguousList.add(
                        RelinkMatchResult(old, MatchConfidence.AMBIGUOUS, best.newBook, candidateScores)
                    )
                } else {
                    matchedList.add(
                        RelinkMatchResult(old, MatchConfidence.MATCHED, best.newBook, candidateScores)
                    )
                    claimedNewIdentifiers.add(best.newBook.identifier)
                }
            }
        }

        val unmatchedNew = newBooks.filter { it.identifier !in claimedNewIdentifiers }

        return RelinkAnalysis(
            matched = matchedList,
            ambiguous = ambiguousList,
            unmatchedOld = unmatchedOldList,
            unmatchedNew = unmatchedNew
        )
    }

    private fun sanitizePath(path: String): String {
        return path.replace('\\', '/').trim('/').trim()
    }

    private fun normalizeText(text: String): String {
        return text.lowercase().replace(Regex("[^a-z0-9]"), "").trim()
    }
}
