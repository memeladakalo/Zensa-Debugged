package com.zensa.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zensa.app.core.network.ApiResult
import com.zensa.app.di.ServiceLocator

/**
 * SyncWorker
 *
 * A WorkManager CoroutineWorker. WorkManager is part of the
 * Android Jetpack SDK and is the recommended way to run
 * deferrable, guaranteed background work. This satisfies part
 * of the Part 2 SDK requirement.
 *
 * Behaviour:
 *   - Runs when the network is available (constraint).
 *   - Calls SyncRepository.pushPending() to flush the sync queue.
 *   - On network failure, returns Result.retry() so WorkManager
 *     reschedules with exponential backoff.
 *   - On success (including "nothing to do"), returns Result.success().
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // ServiceLocator is initialised in ZensaApplication.onCreate()
        // so it is already safe to use here.
        val repo = ServiceLocator.syncRepository

        return try {
            when (val result = repo.pushPending()) {
                is ApiResult.Success -> Result.success()
                is ApiResult.Error -> {
                    // If we're offline, ask WorkManager to retry later.
                    if (!repo.isOnline()) Result.retry()
                    else Result.failure()
                }
                ApiResult.Loading -> Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "zensa_sync"
    }
}
