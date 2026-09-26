package com.example.wifiautomanager.background

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.repository.SettingsRepository
import com.example.wifiautomanager.domain.repository.WifiRepository
import com.example.wifiautomanager.domain.usecase.RunDecisionCycleUseCase
import com.example.wifiautomanager.notification.DecisionNotifier
import com.example.wifiautomanager.util.PermissionHelper
import com.example.wifiautomanager.wifi.ScanState
import com.example.wifiautomanager.wifi.WifiConnectionMonitor
import com.example.wifiautomanager.wifi.WifiScanner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

private const val TAG = "DecisionWorker"
const val PERIODIC_WORK_NAME = "periodic_decision_worker"

@HiltWorker
class DecisionWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val runDecisionCycleUseCase: RunDecisionCycleUseCase,
    private val decisionNotifier: DecisionNotifier,
    private val wifiConnectionMonitor: WifiConnectionMonitor,
    private val wifiScanner: WifiScanner,
    private val settingsRepository: SettingsRepository,
    private val wifiRepository: WifiRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic DecisionWorker background evaluation")

        val settings = settingsRepository.getSettings().firstOrNull()
        if (settings?.autoManagerEnabled == false) {
            Log.d(TAG, "AutoManager is disabled in settings; skipping worker run")
            return Result.success()
        }

        if (!PermissionHelper.hasScanPermissions(appContext)) {
            Log.w(TAG, "Missing scan permissions for background decision cycle")
            return Result.success()
        }

        try {
            val currentState = wifiConnectionMonitor.connectionState.value
            val scanState = wifiScanner.scanState.value
            val candidateScans = if (scanState is ScanState.Results) {
                scanState.networks
            } else {
                emptyList()
            }

            val decision = runDecisionCycleUseCase(currentState, candidateScans)
            Log.d(TAG, "Background decision produced: ${decision.action::class.simpleName} - ${decision.reason}")

            if (decision.action is DecisionAction.SuggestNetwork) {
                decisionNotifier.notifyDecision(decision)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing background decision cycle", e)
            return Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context, intervalMinutes: Long = 15) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()

                val periodicRequest = PeriodicWorkRequestBuilder<DecisionWorker>(
                    intervalMinutes.coerceAtLeast(15), TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicRequest
                )
                Log.d(TAG, "Enqueued periodic WorkManager decision cycle every $intervalMinutes minutes")
            } catch (e: Exception) {
                Log.w(TAG, "WorkManager schedule skipped: ${e.message}")
            }
        }

        fun runOnce(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()

                val request = OneTimeWorkRequestBuilder<DecisionWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "decision_worker_immediate",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (e: Exception) {
                Log.w(TAG, "WorkManager runOnce skipped: ${e.message}")
            }
        }

        fun cancel(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
                Log.d(TAG, "Cancelled periodic WorkManager decision cycle")
            } catch (e: Exception) {
                Log.w(TAG, "WorkManager cancel skipped: ${e.message}")
            }
        }
    }
}
