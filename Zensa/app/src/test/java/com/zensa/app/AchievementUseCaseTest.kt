package com.zensa.app

import com.zensa.app.data.entity.AssignmentEntity
import com.zensa.app.data.entity.StudySessionEntity
import com.zensa.app.data.entity.SubtaskEntity
import com.zensa.app.domain.model.Achievement
import com.zensa.app.domain.usecase.AchievementsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * AchievementsUseCaseTest
 *
 * 12 unit tests for the points and unlock logic. All deterministic —
 * `today` is fixed and the input data is fully controlled.
 */
class AchievementsUseCaseTest {

    private lateinit var useCase: AchievementsUseCase
    private val today = LocalDate.of(2026, 6, 15)

    @Before
    fun setUp() {
        useCase = AchievementsUseCase()
    }

    // 1
    @Test
    fun `empty input returns zero points and no unlocked badges`() {
        val snapshot = useCase.compute(
            assignments = emptyList(),
            subtasks = emptyList(),
            sessions = emptyList(),
            today = today
        )
        assertEquals(0, snapshot.points)
        assertEquals(0, snapshot.unlockedCount)
        assertEquals(0, snapshot.completedAssignments)
        assertEquals(0, snapshot.completedSubtasks)
        assertEquals(0, snapshot.totalStudyMinutes)
    }

    // 2
    @Test
    fun `points are calculated from assignments subtasks and minutes`() {
        val snapshot = useCase.compute(
            assignments = listOf(assignment(completed = true), assignment(completed = true)),
            subtasks = listOf(subtask(completed = true), subtask(completed = true)),
            sessions = listOf(session(minutes = 30), session(minutes = 20)),
            today = today
        )
        // 2*10 + 2*2 + 50*1 = 74
        assertEquals(74, snapshot.points)
    }

    // 3
    @Test
    fun `first study session unlocks after one completed session`() {
        val snapshot = useCase.compute(
            assignments = emptyList(),
            subtasks = emptyList(),
            sessions = listOf(session(minutes = 25)),
            today = today
        )
        val badge = snapshot.achievements
            .first { it.achievement == Achievement.FIRST_STUDY_SESSION }
        assertTrue(badge.unlocked)
    }

    // 4
    @Test
    fun `studied 60 minutes unlocks at exactly 60 minutes`() {
        val snapshot = useCase.compute(
            assignments = emptyList(),
            subtasks = emptyList(),
            sessions = listOf(session(minutes = 30), session(minutes = 30)),
            today = today
        )
        val badge = snapshot.achievements
            .first { it.achievement == Achievement.STUDIED_60_MINUTES }
        assertTrue(badge.unlocked)
    }

    // 5
    @Test
    fun `studied 60 minutes stays locked at 59 minutes`() {
        val snapshot = useCase.compute(
            assignments = emptyList(),
            subtasks = emptyList(),
            sessions = listOf(session(minutes = 59)),
            today = today
        )
        val badge = snapshot.achievements
            .first { it.achievement == Achievement.STUDIED_60_MINUTES }
        assertFalse(badge.unlocked)
    }

    // 6
    @Test
    fun `streak badge unlocks with three consecutive days`() {
        val snapshot = useCase.compute(
            assignments = emptyList(),
            subtasks = emptyList(),
            sessions = listOf(
                session(minutes = 25, startedAt = "2026-06-15T10:00:00"),
                session(minutes = 25, startedAt = "2026-06-14T10:00:00"),
                session(minutes = 25, startedAt = "2026-06-13T10:00:00")
            ),
            today = today
        )
        val badge = snapshot.achievements
            .first { it.achievement == Achievement.STREAK_3_DAYS }
        assertTrue(badge.unlocked)
        assertEquals(3, snapshot.currentStreakDays)
    }

    // 7
    @Test
    fun `first assignment badge requires a completed assignment`() {
        val locked = useCase.compute(
            assignments = listOf(assignment(completed = false)),
            subtasks = emptyList(),
            sessions = emptyList(),
            today = today
        )
        val lockedBadge = locked.achievements
            .first { it.achievement == Achievement.FIRST_ASSIGNMENT_COMPLETED }
        assertFalse(lockedBadge.unlocked)

        val unlocked = useCase.compute(
            assignments = listOf(assignment(completed = true)),
            subtasks = emptyList(),
            sessions = emptyList(),
            today = today
        )
        val unlockedBadge = unlocked.achievements
            .first { it.achievement == Achievement.FIRST_ASSIGNMENT_COMPLETED }
        assertTrue(unlockedBadge.unlocked)
    }

    // 8
    @Test
    fun `five assignments badge unlocks at exactly five`() {
        val four = useCase.compute(
            assignments = (1..4).map { assignment(completed = true) },
            subtasks = emptyList(),
            sessions = emptyList(),
            today = today
        )
        val fourBadge = four.achievements
            .first { it.achievement == Achievement.FIVE_ASSIGNMENTS_COMPLETED }
        assertFalse(fourBadge.unlocked)

        val five = useCase.compute(
            assignments = (1..5).map { assignment(completed = true) },
            subtasks = emptyList(),
            sessions = emptyList(),
            today = today
        )
        val fiveBadge = five.achievements
            .first { it.achievement == Achievement.FIVE_ASSIGNMENTS_COMPLETED }
        assertTrue(fiveBadge.unlocked)
    }

    // 9
    @Test
    fun `ten subtasks badge counts only completed subtasks`() {
        val snapshot = useCase.compute(
            assignments = emptyList(),
            subtasks = (1..10).map { subtask(completed = it != 10) },
            sessions = emptyList(),
            today = today
        )
        val badge = snapshot.achievements
            .first { it.achievement == Achievement.TEN_SUBTASKS_COMPLETED }
        assertFalse(badge.unlocked)

        val allDone = useCase.compute(
            assignments = emptyList(),
            subtasks = (1..10).map { subtask(completed = true) },
            sessions = emptyList(),
            today = today
        )
        val doneBadge = allDone.achievements
            .first { it.achievement == Achievement.TEN_SUBTASKS_COMPLETED }
        assertTrue(doneBadge.unlocked)
    }

    // 10
    @Test
    fun `soft deleted rows are ignored`() {
        val snapshot = useCase.compute(
            assignments = listOf(assignment(completed = true, deleted = true)),
            subtasks = listOf(subtask(completed = true, deleted = true)),
            sessions = listOf(session(minutes = 60, deleted = true)),
            today = today
        )
        assertEquals(0, snapshot.points)
        assertEquals(0, snapshot.unlockedCount)
    }

    // 11
    @Test
    fun `passed in unlock dates are preserved on unlocked badges`() {
        val snapshot = useCase.compute(
            assignments = emptyList(),
            subtasks = emptyList(),
            sessions = listOf(session(minutes = 25)),
            unlockedAtMap = mapOf(
                Achievement.FIRST_STUDY_SESSION.id to "2026-06-10T09:00:00"
            ),
            today = today
        )
        val badge = snapshot.achievements
            .first { it.achievement == Achievement.FIRST_STUDY_SESSION }
        assertTrue(badge.unlocked)
        assertEquals("2026-06-10T09:00:00", badge.unlockedAt)
    }

    // 12
    @Test
    fun `compute is deterministic for identical input`() {
        val a = useCase.compute(
            assignments = listOf(assignment(completed = true)),
            subtasks = emptyList(),
            sessions = listOf(session(minutes = 30)),
            today = today
        )
        val b = useCase.compute(
            assignments = listOf(assignment(completed = true)),
            subtasks = emptyList(),
            sessions = listOf(session(minutes = 30)),
            today = today
        )
        assertEquals(a.points, b.points)
        assertEquals(a.unlockedCount, b.unlockedCount)
    }

    // ---------- Fixtures ----------

    private fun assignment(
        completed: Boolean = false,
        deleted: Boolean = false
    ) = AssignmentEntity(
        localId = "a-${System.nanoTime()}",
        serverId = null,
        userId = "user-1",
        moduleLocalId = null,
        title = "Test",
        description = "",
        priority = "MEDIUM",
        status = if (completed) "COMPLETED" else "PENDING",
        dueDate = "2026-06-20T23:59:00",
        isCompleted = completed,
        createdAt = "2026-06-01T00:00:00",
        updatedAt = "2026-06-01T00:00:00",
        isSynced = false,
            private fun assignment(
        completed: Boolean = false,
        deleted: Boolean = false
    ) = AssignmentEntity(
        localId = "a-${System.nanoTime()}",
        serverId = null,
        userId = "user-1",
        moduleLocalId = null,
        title = "Test",
        description = "",
        priority = "MEDIUM",
        status = if (completed) "COMPLETED" else "PENDING",
        dueDate = "2026-06-20T23:59:00",
        isCompleted = completed,
        createdAt = "2026-06-01T00:00:00",
        updatedAt = "2026-06-01T00:00:00",
        isSynced = false,
        isDeleted = deleted
    )

    private fun subtask(
        completed: Boolean = false,
        deleted: Boolean = false
    ) = SubtaskEntity(
        localId = "s-${System.nanoTime()}",
        serverId = null,
        assignmentLocalId = "parent",
        title = "Subtask",
        position = 0,
        isCompleted = completed,
        createdAt = "2026-06-01T00:00:00",
        updatedAt = "2026-06-01T00:00:00",
        isSynced = false,
        isDeleted = deleted
    )

    private fun session(
        minutes: Int = 25,
        startedAt: String = "2026-06-15T10:00:00",
        deleted: Boolean = false
    ) = StudySessionEntity(
        localId = "ss-${System.nanoTime()}",
        serverId = null,
        userId = "user-1",
        moduleLocalId = null,
        startedAt = startedAt,
        endedAt = startedAt,
        durationSeconds = minutes * 60,
        completed = true,
        createdAt = startedAt,
        updatedAt = startedAt,
        isSynced = false,
        isDeleted = deleted
    )
}
