package com.zensa.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * SyncScheduler — registers the SyncWorker with WorkManager.
 *
 * Two kinds of work are registered:
 *   - Periodic: runs every 15 minutes as a safety net.
 *   - One-shot: triggered by the app whenever the user performs
 *     an offline-capable write, for immediate sync when online.
 */
object SyncScheduler {

    private const val PERIODIC_WORK_NAME = "zensa_sync_periodic"

    /** Constraint: only run when any network is available. */
    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Schedules a periodic background sync. Safe to call on every
     * app launch — KEEP policy means it won't reset the schedule.
     */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Enqueues a one-shot sync immediately. Called after any local
     * write so the change is pushed as soon as possible.
     *
     * Uses KEEP so we don't stack up many requests if the user taps
     * quickly.
     */
    fun requestImmediateSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15, TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
