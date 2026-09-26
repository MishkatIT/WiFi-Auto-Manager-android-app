package com.example.wifiautomanager.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

// Periodic DecisionWorker for background Wi-Fi evaluation - implemented in Phase 8
class DecisionWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return Result.success()
    }
}
