package com.zensa.app.domain.usecase

import com.zensa.app.data.entity.AssignmentEntity
import com.zensa.app.data.entity.SubtaskEntity
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * SmartFocusUseCase
 *
 * Pure business logic — no Android, no Room, no Retrofit.
 * Given a list of assignments and a lookup of subtasks, it ranks
 * them by a "focus score" so the dashboard shows the most
 * important work first.
 *
 * The algorithm is rule-based and explainable:
 *
 *   score = deadlineScore
 *         + priorityScore
 *         + progressScore
 *         + completionPenalty
 *
 * Higher score = more urgent / more worth focusing on right now.
 *
 * Design notes:
 *  - The calculation is deterministic (same input → same output).
 *  - All weightings are declared as named constants so they can
 *    be tuned in one place and explained in an assessment.
 *  - Completed assignments are excluded from the returned list
 *    entirely (we do not just rank them low).
 */
class SmartFocusUseCase {

    /**
     * @param assignments every candidate assignment.
     * @param subtasksByAssignment map of assignmentLocalId →
     *        that assignment's subtasks. Used to compute progress.
     * @param limit maximum number of items to return (default 5).
     * @param today the "now" reference point. Injected so tests are
     *        deterministic.
     * @return the top-N highest-scoring assignments, ordered by score
     *         descending. Ties are broken by earliest due date.
     */
    fun rank(
        assignments: List<AssignmentEntity>,
        subtasksByAssignment: Map<String, List<SubtaskEntity>> = emptyMap(),
        limit: Int = DEFAULT_LIMIT,
        today: LocalDate = LocalDate.now()
    ): List<FocusedAssignment> {

        return assignments
            .asSequence()
            .filter { !it.isCompleted && !it.isDeleted }
            .map { a ->
                val subtasks = subtasksByAssignment[a.localId].orEmpty()
                FocusedAssignment(
                    assignment = a,
                    score = score(a, subtasks, today),
                    daysUntilDue = daysUntilDue(a.dueDate, today)
                )
            }
            .sortedWith(
                compareByDescending<FocusedAssignment> { it.score }
                    .thenBy { it.daysUntilDue }
            )
            .take(limit)
            .toList()
    }

    /**
     * The scoring function. Exposed so it can be unit-tested
     * directly without running the whole ranking.
     */
    fun score(
        assignment: AssignmentEntity,
        subtasks: List<SubtaskEntity> = emptyList(),
        today: LocalDate = LocalDate.now()
    ): Int {
        val days = daysUntilDue(assignment.dueDate, today)
        return deadlineScore(days) +
            priorityScore(assignment.priority) +
            progressScore(subtasks) +
            statusAdjustment(assignment.status)
    }

    // ---------------- Component scores ----------------

    /**
     * Earlier due date → higher score.
     * Anything overdue scores highest.
     */
    internal fun deadlineScore(daysUntilDue: Long): Int = when {
        daysUntilDue < 0L    -> OVERDUE_SCORE
        daysUntilDue == 0L   -> DUE_TODAY_SCORE
        daysUntilDue <= 2L   -> DUE_SOON_SCORE
        daysUntilDue <= 7L   -> DUE_THIS_WEEK_SCORE
        daysUntilDue <= 30L  -> DUE_THIS_MONTH_SCORE
        else                 -> DUE_LATER_SCORE
    }

    /**
     * Higher priority → higher score.
     * Unknown values fall back to MEDIUM.
     */
    internal fun priorityScore(priority: String): Int = when (priority.uppercase()) {
        "LOW"    -> PRIORITY_LOW
        "MEDIUM" -> PRIORITY_MEDIUM
        "HIGH"   -> PRIORITY_HIGH
        "URGENT" -> PRIORITY_URGENT
        else     -> PRIORITY_MEDIUM
    }

    /**
     * Assignments with subtasks get a bonus, so large multi-step
     * work is nudged up the list. This reflects the fact that
     * decomposing a task means the student has committed to it.
     */
    internal fun progressScore(subtasks: List<SubtaskEntity>): Int = when {
        subtasks.isEmpty() -> 0
        subtasks.all { it.isCompleted } -> SUBTASK_BONUS / 2
        else -> SUBTASK_BONUS
    }

    /**
     * Small nudge for assignments the user has already started.
     */
    internal fun statusAdjustment(status: String): Int = when (status.uppercase()) {
        "IN_PROGRESS" -> STATUS_IN_PROGRESS
        else -> 0
    }

    // ---------------- Date helper ----------------

    /**
     * Whole days between today and the assignment's due date.
     * Negative if the due date is in the past.
     */
    internal fun daysUntilDue(dueDateIso: String, today: LocalDate): Long {
        return try {
            val datePart = dueDateIso.substringBefore("T")
            val due = LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE)
            Duration.between(today.atStartOfDay(), due.atStartOfDay()).toDays()
        } catch (e: Exception) {
            // Malformed date — treat as "far away" so it doesn't
            // drown out genuinely urgent items.
            Long.MAX_VALUE
        }
    }

    // ---------------- Constants ----------------

    companion object {
        const val DEFAULT_LIMIT = 5

        // Deadline weights
        const val OVERDUE_SCORE         = 100
        const val DUE_TODAY_SCORE       = 80
        const val DUE_SOON_SCORE        = 60
        const val DUE_THIS_WEEK_SCORE   = 40
        const val DUE_THIS_MONTH_SCORE  = 20
        const val DUE_LATER_SCORE       = 5

        // Priority weights
        const val PRIORITY_LOW     = 0
        const val PRIORITY_MEDIUM  = 10
        const val PRIORITY_HIGH    = 20
        const val PRIORITY_URGENT  = 30

        // Bonus for having subtasks
        const val SUBTASK_BONUS = 10

        // Nudge for in-progress work
        const val STATUS_IN_PROGRESS = 5
    }
}

/**
 * One ranked entry, returned by SmartFocusUseCase.rank().
 */
data class FocusedAssignment(
    val assignment: AssignmentEntity,
    val score: Int,
    val daysUntilDue: Long
)
