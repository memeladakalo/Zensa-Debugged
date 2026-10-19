package com.zensa.app.domain.usecase

import com.zensa.app.data.entity.StudySessionEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * StudySessionStatsUseCase
 *
 * Pure Kotlin. No Android. No Room. No Retrofit.
 *
 * Given a list of study sessions, computes:
 *   - Total completed minutes
 *   - Total completed sessions
 *   - Current study streak (consecutive days ending today)
 *   - Longest streak ever
 *   - Sessions completed today
 *
 * All logic is deterministic. `today` is injected so tests never
 * depend on the real clock.
 */
class StudySessionStatsUseCase {

    private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * One-shot summary for the Dashboard and Achievements screens.
     */
    fun summarise(
        sessions: List<StudySessionEntity>,
        today: LocalDate = LocalDate.now()
    ): StudyStats {
        val completed = sessions.filter { it.completed && !it.isDeleted }

        val totalSeconds = completed.sumOf { it.durationSeconds }
        val totalMinutes = totalSeconds / 60

        val sessionDates = completed
            .mapNotNull { parseDate(it.startedAt) }
            .distinct()
            .sorted()

        val currentStreak = currentStreak(sessionDates, today)
        val longestStreak = longestStreak(sessionDates)
        val sessionsToday = completed.count { parseDate(it.startedAt) == today }

        return StudyStats(
            totalMinutes = totalMinutes,
            totalSessions = completed.size,
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            sessionsToday = sessionsToday
        )
    }

    /**
     * Number of consecutive days up to and including today that have
     * at least one completed session. Yesterday counts as continuing
     * the streak (a user who studied yesterday but hasn't yet today
     * still has an active streak).
     */
    internal fun currentStreak(dates: List<LocalDate>, today: LocalDate): Int {
        if (dates.isEmpty()) return 0

        val dateSet = dates.toHashSet()
        val anchor = when {
            dateSet.contains(today) -> today
            dateSet.contains(today.minusDays(1)) -> today.minusDays(1)
            else -> return 0
        }

        var streak = 0
        var cursor = anchor
        while (dateSet.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /**
     * Longest run of consecutive dates anywhere in the list.
     */
    internal fun longestStreak(dates: List<LocalDate>): Int {
        if (dates.isEmpty()) return 0

        val sorted = dates.distinct().sorted()
        var longest = 1
        var current = 1

        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1].plusDays(1)) {
                current++
                if (current > longest) longest = current
            } else {
                current = 1
            }
        }
        return longest
    }

    /**
     * Extracts the date part from an ISO date-time string, tolerating
     * malformed input by returning null.
     */
    internal fun parseDate(iso: String): LocalDate? = try {
        LocalDate.parse(iso.substringBefore("T"), isoDate)
    } catch (_: Exception) {
        null
    }
}

/**
 * Result of a Smart-Study summary.
 */
data class StudyStats(
    val totalMinutes: Int = 0,
    val totalSessions: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val sessionsToday: Int = 0
)
