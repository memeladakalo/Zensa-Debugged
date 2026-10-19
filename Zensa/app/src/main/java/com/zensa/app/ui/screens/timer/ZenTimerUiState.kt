package com.zensa.app.ui.screens.timer

import com.zensa.app.data.entity.ModuleEntity

/**
 * Everything the Zen Timer screen needs to render itself.
 */
data class ZenTimerUiState(

    /** Chosen duration in minutes, e.g. 25. */
    val selectedMinutes: Int = 25,

    /** Seconds remaining in the current countdown. */
    val secondsRemaining: Int = 25 * 60,

    /** Total seconds for the current run (for progress). */
    val totalSeconds: Int = 25 * 60,

    /** Whether the timer is currently counting down. */
    val isRunning: Boolean = false,

    /** Whether the timer is paused mid-session. */
    val isPaused: Boolean = false,

    /** True once the user has completed a full session this run. */
    val isFinished: Boolean = false,

    /** ISO date-time of the current session's start. */
    val startedAtIso: String? = null,

    /** Chosen module for this session (nullable). */
    val selectedModuleLocalId: String? = null,

    /** Modules the user can attach this session to. */
    val availableModules: List<ModuleEntity> = emptyList(),

    /** A message to show after recording a session. */
    val infoMessage: String? = null
) {
    /** Progress in 0.0..1.0 for the circular indicator. */
    val progress: Float
        get() = if (totalSeconds <= 0) 0f
        else ((totalSeconds - secondsRemaining).toFloat() / totalSeconds).coerceIn(0f, 1f)

    /** Formatted MM:SS for display. */
    val timeText: String
        get() {
            val m = secondsRemaining / 60
            val s = secondsRemaining % 60
            return "%02d:%02d".format(m, s)
        }
}
