package com.zensa.app.domain.model

/**
 * Achievement
 *
 * The catalogue of unlockable badges in Zensa.
 *
 * Each badge has:
 *   - a stable id (used as the primary key in Room and the JSON
 *     contract with the server)
 *   - a title and description for the UI
 *   - a threshold value (points / sessions / streak days etc.)
 *
 * A badge is "unlocked" when the metric it depends on reaches or
 * exceeds its threshold. Nothing is stored per-badge except the
 * unlock date — the metric is always computed live from data the
 * app already has.
 *
 * Keeping the catalogue here (rather than in Room) means we can
 * add, remove, or tune badges without a database migration.
 */
enum class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val threshold: Int
) {

    // ---- Study time ----
    FIRST_STUDY_SESSION(
        id = "first_study_session",
        title = "First Step",
        description = "Complete your first study session",
        threshold = 1
    ),
    STUDIED_60_MINUTES(
        id = "studied_60_minutes",
        title = "One Hour In",
        description = "Study for a total of 60 minutes",
        threshold = 60
    ),
    STUDIED_600_MINUTES(
        id = "studied_600_minutes",
        title = "Ten Hour Club",
        description = "Study for a total of 10 hours",
        threshold = 600
    ),

    // ---- Streaks ----
    STREAK_3_DAYS(
        id = "streak_3_days",
        title = "Warming Up",
        description = "Study for 3 days in a row",
        threshold = 3
    ),
    STREAK_7_DAYS(
        id = "streak_7_days",
        title = "One Week Strong",
        description = "Study for 7 days in a row",
        threshold = 7
    ),
    STREAK_30_DAYS(
        id = "streak_30_days",
        title = "Habit Formed",
        description = "Study for 30 days in a row",
        threshold = 30
    ),

    // ---- Assignments ----
    FIRST_ASSIGNMENT_COMPLETED(
        id = "first_assignment_completed",
        title = "Getting Things Done",
        description = "Complete your first assignment",
        threshold = 1
    ),
    FIVE_ASSIGNMENTS_COMPLETED(
        id = "five_assignments_completed",
        title = "Building Momentum",
        description = "Complete 5 assignments",
        threshold = 5
    ),
    TWENTY_ASSIGNMENTS_COMPLETED(
        id = "twenty_assignments_completed",
        title = "Assignment Slayer",
        description = "Complete 20 assignments",
        threshold = 20
    ),

    // ---- Subtasks ----
    TEN_SUBTASKS_COMPLETED(
        id = "ten_subtasks_completed",
        title = "Step by Step",
        description = "Complete 10 subtasks",
        threshold = 10
    ),
    FIFTY_SUBTASKS_COMPLETED(
        id = "fifty_subtasks_completed",
        title = "Detail Master",
        description = "Complete 50 subtasks",
        threshold = 50
    )
}

/**
 * A resolved badge — the catalogue entry plus whether the user has
 * unlocked it, and when.
 *
 * Returned by AchievementsUseCase; consumed by the Achievements
 * screen and the Dashboard.
 */
data class UnlockedAchievement(
    val achievement: Achievement,
    val unlocked: Boolean,
    /** ISO date-time when unlocked, or null while still locked. */
    val unlockedAt: String?
)

/**
 * A complete snapshot of the user's gamification state.
 */
data class AchievementSnapshot(
    val points: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val totalStudyMinutes: Int,
    val completedAssignments: Int,
    val completedSubtasks: Int,
    val achievements: List<UnlockedAchievement>
) {
    val unlockedCount: Int
        get() = achievements.count { it.unlocked }

    val totalCount: Int
        get() = achievements.size
}
