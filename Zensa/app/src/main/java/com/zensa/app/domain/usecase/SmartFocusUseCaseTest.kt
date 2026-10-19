package com.zensa.app.domain.usecase

import com.zensa.app.data.entity.AssignmentEntity
import com.zensa.app.data.entity.SubtaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * SmartFocusUseCaseTest
 *
 * Unit tests for the Smart Focus ranking algorithm.
 *
 * These tests are deliberately exhaustive because Smart Focus is
 * one of the three User-Defined Features. Every public behaviour
 * of the algorithm is asserted here:
 *
 *   1.  Overdue assignments outrank everything else
 *   2.  Due today beats due this week
 *   3.  Higher priority beats lower priority (same due date)
 *   4.  Urgent beats high beats medium beats low
 *   5.  Assignments with subtasks get a bonus
 *   6.  Assignments whose subtasks are all done lose the bonus
 *   7.  In-progress status nudges the score up
 *   8.  Completed assignments are excluded from the ranking
 *   9.  Soft-deleted assignments are excluded from the ranking
 *   10. Ranking is ordered by score, then by earliest due date
 *   11. The list is capped by the 'limit' parameter
 *   12. Malformed due dates do not crash — they just rank last
 *   13. Same input → same output (determinism)
 *   14. daysUntilDue handles overdue, today, future, and malformed
 */
class SmartFocusUseCaseTest {

    private lateinit var useCase: SmartFocusUseCase

    /** Fixed "today" so tests never depend on the real clock. */
    private val today = LocalDate.of(2026, 5, 10)

    @Before
    fun setUp() {
        useCase = SmartFocusUseCase()
    }

    // ---------- Test 1: overdue beats due-today ----------

    @Test
    fun `overdue assignment scores higher than due today`() {
        val overdue = assignment(dueDate = "2026-05-08", priority = "MEDIUM")
        val dueToday = assignment(dueDate = "2026-05-10", priority = "MEDIUM")

        val overdueScore = useCase.score(overdue, emptyList(), today)
        val dueTodayScore = useCase.score(dueToday, emptyList(), today)

        assertTrue(
            "Overdue score ($overdueScore) should exceed due-today ($dueTodayScore)",
            overdueScore > dueTodayScore
        )
    }

    // ---------- Test 2: due today beats due this week ----------

    @Test
    fun `due today scores higher than due in five days`() {
        val todayA = assignment(dueDate = "2026-05-10")
        val inFiveDays = assignment(dueDate = "2026-05-15")

        val todayScore = useCase.score(todayA, emptyList(), today)
        val laterScore = useCase.score(inFiveDays, emptyList(), today)

        assertTrue(todayScore > laterScore)
    }

    // ---------- Test 3: priority affects score ----------

    @Test
    fun `urgent outranks high outranks medium outranks low`() {
        val due = "2026-05-15"

        val urgent = useCase.score(assignment(dueDate = due, priority = "URGENT"))
        val high   = useCase.score(assignment(dueDate = due, priority = "HIGH"))
        val medium = useCase.score(assignment(dueDate = due, priority = "MEDIUM"))
        val low    = useCase.score(assignment(dueDate = due, priority = "LOW"))

        assertTrue(urgent > high)
        assertTrue(high > medium)
        assertTrue(medium > low)
    }

    // ---------- Test 4: unknown priority falls back to MEDIUM ----------

    @Test
    fun `unknown priority falls back to medium`() {
        val due = "2026-05-15"
        val unknown = useCase.score(assignment(dueDate = due, priority = "SUPER_URGENT"))
        val medium = useCase.score(assignment(dueDate = due, priority = "MEDIUM"))

        assertEquals(medium, unknown)
    }

    // ---------- Test 5: subtask bonus ----------

    @Test
    fun `having subtasks adds a bonus`() {
        val a = assignment(dueDate = "2026-05-15", priority = "MEDIUM")
        val noSubtasks = useCase.score(a, emptyList(), today)

        val subtasks = listOf(
            subtask("s1", completed = false),
            subtask("s2", completed = false)
        )
        val withSubtasks = useCase.score(a, subtasks, today)

        assertTrue(withSubtasks > noSubtasks)
    }

    // ---------- Test 6: completed subtasks reduce the bonus ----------

    @Test
    fun `all subtasks completed loses half the subtask bonus`() {
        val a = assignment(dueDate = "2026-05-15")

        val pendingSubtasks = listOf(
            subtask("s1", completed = false),
            subtask("s2", completed = false)
        )
        val allDoneSubtasks = listOf(
            subtask("s1", completed = true),
            subtask("s2", completed = true)
        )

        val pendingScore = useCase.score(a, pendingSubtasks, today)
        val doneScore = useCase.score(a, allDoneSubtasks, today)

        assertTrue(
            "Fully completed subtasks should score less than pending ones",
            doneScore < pendingScore
        )
    }

    // ---------- Test 7: in-progress nudges the score ----------

    @Test
    fun `in-progress assignments score higher than pending`() {
        val pending = useCase.score(
            assignment(dueDate = "2026-05-15", status = "PENDING"),
            emptyList(),
            today
        )
        val inProgress = useCase.score(
            assignment(dueDate = "2026-05-15", status = "IN_PROGRESS"),
            emptyList(),
            today
        )

        assertTrue(inProgress > pending)
    }

    // ---------- Test 8: completed assignments excluded ----------

    @Test
    fun `completed assignments are not returned in the ranking`() {
        val done = assignment(
            localId = "done",
            dueDate = "2026-05-10",
            isCompleted = true
        )
        val live = assignment(
            localId = "live",
            dueDate = "2026-05-20"
        )

        val ranked = useCase.rank(listOf(done, live), emptyMap(), limit = 10, today = today)

        assertEquals(1, ranked.size)
        assertEquals("live", ranked.first().assignment.localId)
    }

    // ---------- Test 9: soft-deleted assignments excluded ----------

    @Test
    fun `soft-deleted assignments are not returned`() {
        val deleted = assignment(
            localId = "deleted",
            dueDate = "2026-05-10",
            isDeleted = true
        )
        val live = assignment(
            localId = "live",
            dueDate = "2026-05-15"
        )

        val ranked = useCase.rank(listOf(deleted, live), emptyMap(), limit = 10, today = today)

        assertEquals(1, ranked.size)
        assertEquals("live", ranked.first().assignment.localId)
    }

    // ---------- Test 10: ordering and tie-breaking ----------

    @Test
    fun `assignments are ordered by score descending then earliest due date`() {
        val low = assignment(
            localId = "low",
            dueDate = "2026-05-25",
            priority = "LOW"
        )
        val urgent = assignment(
            localId = "urgent",
            dueDate = "2026-05-20",
            priority = "URGENT"
        )
        val alsoUrgent = assignment(
            localId = "alsoUrgent",
            dueDate = "2026-05-12",
            priority = "URGENT"
        )

        val ranked = useCase.rank(
            listOf(low, urgent, alsoUrgent),
            emptyMap(),
            limit = 10,
            today = today
        )

        // The two urgent items should be at the top, tied on score.
        // Between them, earlier due date first.
        assertEquals("alsoUrgent", ranked[0].assignment.localId)
        assertEquals("urgent", ranked[1].assignment.localId)
        assertEquals("low", ranked[2].assignment.localId)
    }

    // ---------- Test 11: limit parameter ----------

    @Test
    fun `limit parameter caps the returned list`() {
        val many = (1..10).map { i ->
            assignment(localId = "a$i", dueDate = "2026-05-${10 + i % 15}")
        }

        val ranked = useCase.rank(many, emptyMap(), limit = 3, today = today)

        assertEquals(3, ranked.size)
    }

    // ---------- Test 12: malformed due date does not crash ----------

    @Test
    fun `malformed due date does not crash and ranks last`() {
        val broken = assignment(localId = "broken", dueDate = "not-a-date")
        val valid = assignment(localId = "valid", dueDate = "2026-05-15")

        val ranked = useCase.rank(
            listOf(broken, valid),
            emptyMap(),
            limit = 10,
            today = today
        )

        assertEquals(2, ranked.size)
        assertEquals("valid", ranked.first().assignment.localId)
    }

    // ---------- Test 13: determinism ----------

    @Test
    fun `ranking is deterministic for the same input`() {
        val items = listOf(
            assignment(localId = "a", dueDate = "2026-05-15", priority = "HIGH"),
            assignment(localId = "b", dueDate = "2026-05-12", priority = "URGENT"),
            assignment(localId = "c", dueDate = "2026-06-01", priority = "LOW")
        )

        val first = useCase.rank(items, emptyMap(), limit = 10, today = today)
        val second = useCase.rank(items, emptyMap(), limit = 10, today = today)

        assertEquals(first.map { it.assignment.localId }, second.map { it.assignment.localId })
    }

    // ---------- Test 14: daysUntilDue handles every case ----------

    @Test
    fun `daysUntilDue returns correct values`() {
        assertEquals(0L, useCase.daysUntilDue("2026-05-10", today))
        assertEquals(5L, useCase.daysUntilDue("2026-05-15", today))
        assertEquals(-2L, useCase.daysUntilDue("2026-05-08", today))
        assertEquals(0L, useCase.daysUntilDue("2026-05-10T23:59:00", today))
        assertEquals(Long.MAX_VALUE, useCase.daysUntilDue("garbage", today))
    }

    // ---------------- Test fixtures ----------------

    private fun assignment(
        localId: String = "id-${System.nanoTime()}",
        dueDate: String,
        priority: String = "MEDIUM",
        status: String = "PENDING",
        isCompleted: Boolean = false,
        isDeleted: Boolean = false
    ) = AssignmentEntity(
        localId = localId,
        serverId = null,
        userId = "user-1",
        moduleLocalId = null,
        title = "Test assignment",
        description = "",
        priority = priority,
        status = status,
        dueDate = dueDate,
        isCompleted = isCompleted,
        createdAt = "2026-05-01T00:00:00",
        updatedAt = "2026-05-01T00:00:00",
        isSynced = false,
        isDeleted = isDeleted
    )

    private fun subtask(
        localId: String,
        completed: Boolean
    ) = SubtaskEntity(
        localId = localId,
        serverId = null,
        assignmentLocalId = "parent",
        title = "Subtask",
        position = 0,
        isCompleted = completed,
        createdAt = "2026-05-01T00:00:00",
        updatedAt = "2026-05-01T00:00:00",
        isSynced = false,
        isDeleted = false
    )
}
