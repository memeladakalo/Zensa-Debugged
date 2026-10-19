package com.zensa.app.domain.usecase

import com.zensa.app.data.entity.StudySessionEntity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * StudySessionStatsUseCaseTest
 *
 * 12 unit tests for the streak and totals logic.
 *
 * Every test fixes `today` so it never depends on the real clock.
 */
class StudySessionStatsUseCaseTest {

    private lateinit var useCase: StudySessionStatsUseCase

    private val today = LocalDate.of(2026, 6, 15)

    @Before
    fun setUp() {
        useCase = StudySessionStatsUseCase()
    }

    // 1
    @Test
    fun `empty list produces zero stats`() {
        val stats = useCase.summarise(emptyList(), today)
        assertEquals(0, stats.totalMinutes)
        assertEquals(0, stats.totalSessions)
        assertEquals(0, stats.currentStreakDays)
        assertEquals(0, stats.longestStreakDays)
        assertEquals(0, stats.sessionsToday)
    }

    // 2
    @Test
    fun `total minutes sums completed sessions only`() {
        val sessions = listOf(
            session("2026-06-15T10:00:00", minutes = 25, completed = true),
            session("2026-06-15T12:00:00", minutes = 50, completed = true),
            session("2026-06-15T14:00:00", minutes = 30, completed = false)
        )
        val stats = useCase.summarise(sessions, today)
        assertEquals(75, stats.totalMinutes)
        assertEquals(2, stats.totalSessions)
    }

    // 3
    @Test
    fun `sessions today counted correctly`() {
        val sessions = listOf(
            session("2026-06-15T10:00:00", minutes = 25, completed = true),
            session("2026-06-15T14:00:00", minutes = 30, completed = true),
            session("2026-06-14T10:00:00", minutes = 25, completed = true)
        )
        assertEquals(2, useCase.summarise(sessions, today).sessionsToday)
    }

    // 4
    @Test
    fun `current streak counts today backwards`() {
        val sessions = listOf(
            session("2026-06-15T10:00:00", minutes = 25),
            session("2026-06-14T10:00:00", minutes = 25),
            session("2026-06-13T10:00:00", minutes = 25),
            session("2026-06-11T10:00:00", minutes = 25)
        )
        assertEquals(3, useCase.summarise(sessions, today).currentStreakDays)
    }

    // 5
    @Test
    fun `current streak includes yesterday if today has no session`() {
        val sessions = listOf(
            session("2026-06-14T10:00:00", minutes = 25),
            session("2026-06-13T10:00:00", minutes = 25)
        )
        assertEquals(2, useCase.summarise(sessions, today).currentStreakDays)
    }

    // 6
    @Test
    fun `current streak is zero when most recent session is older than yesterday`() {
        val sessions = listOf(
            session("2026-06-10T10:00:00", minutes = 25),
            session("2026-06-09T10:00:00", minutes = 25)
        )
        assertEquals(0, useCase.summarise(sessions, today).currentStreakDays)
    }

    // 7
    @Test
    fun `longest streak finds the historical best run`() {
        val sessions = listOf(
            session("2026-06-15T10:00:00"),
            session("2026-06-14T10:00:00"),
            session("2026-06-09T10:00:00"),
            session("2026-06-08T10:00:00"),
            session("2026-06-07T10:00:00"),
            session("2026-06-06T10:00:00"),
            session("2026-06-05T10:00:00")
        )
        assertEquals(5, useCase.summarise(sessions, today).longestStreakDays)
    }

    // 8
    @Test
    fun `longest streak with no consecutive days is one`() {
        val sessions = listOf(
            session("2026-06-15T10:00:00"),
            session("2026-06-10T10:00:00"),
            session("2026-06-05T10:00:00")
        )
        assertEquals(1, useCase.summarise(sessions, today).longestStreakDays)
    }

    // 9
    @Test
    fun `multiple sessions on the same day count as one streak day`() {
        val sessions = listOf(
            session("2026-06-15T08:00:00"),
            session("2026-06-15T14:00:00"),
            session("2026-06-15T20:00:00")
        )
        assertEquals(1, useCase.summarise(sessions, today).currentStreakDays)
    }

    // 10
    @Test
    fun `soft-deleted sessions are excluded`() {
        val deleted = session("2026-06-15T10:00:00", minutes = 60)
            .copy(isDeleted = true)
        val live = session("2026-06-15T12:00:00", minutes = 30)
        val stats = useCase.summarise(listOf(deleted, live), today)
        assertEquals(30, stats.totalMinutes)
        assertEquals(1, stats.totalSessions)
    }

    // 11
    @Test
    fun `malformed dates do not crash and are ignored`() {
        val broken = session("not-a-date", minutes = 25)
        val valid = session("2026-06-15T10:00:00", minutes = 25)
        val stats = useCase.summarise(listOf(broken, valid), today)
        assertEquals(25, stats.totalMinutes)
        assertEquals(1, stats.sessionsToday)
    }

    // 12
    @Test
    fun `compute is deterministic for same input`() {
        val sessions = listOf(
            session("2026-06-15T10:00:00", minutes = 25),
            session("2026-06-14T10:00:00", minutes = 25),
            session("2026-06-13T10:00:00", minutes = 25)
        )
        val a = useCase.summarise(sessions, today)
        val b = useCase.summarise(sessions, today)
        assertEquals(a, b)
    }

    // ---------- Fixtures ----------

    private fun session(
        startedAt: String,
        minutes: Int = 25,
        completed: Boolean = true
    ) = StudySessionEntity(
        localId = "s-${System.nanoTime()}",
        serverId = null,
        userId = "user-1",
        moduleLocalId = null,
        startedAt = startedAt,
        endedAt = if (completed) startedAt else null,
        durationSeconds = minutes * 60,
        completed = completed,
        createdAt = startedAt,
        updatedAt = startedAt,
        isSynced = false,
        isDeleted = false
    )
}
