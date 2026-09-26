package com.example.wifiautomanager.background

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.wifiautomanager.app.MainActivity
import com.example.wifiautomanager.domain.model.DecisionAction
import com.example.wifiautomanager.domain.model.WifiState
import com.example.wifiautomanager.domain.repository.SettingsRepository
import com.example.wifiautomanager.domain.repository.WifiRepository
import com.example.wifiautomanager.domain.usecase.RunDecisionCycleUseCase
import com.example.wifiautomanager.notification.DecisionNotifier
import com.example.wifiautomanager.notification.NotificationChannels
import com.example.wifiautomanager.wifi.ScanState
import com.example.wifiautomanager.wifi.WifiConnectionMonitor
import com.example.wifiautomanager.wifi.WifiScanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WifiForegroundService"
private const val FOREGROUND_NOTIFICATION_ID = 3001

@AndroidEntryPoint
class WifiForegroundService : Service() {

    @Inject
    lateinit var wifiConnectionMonitor: WifiConnectionMonitor

    @Inject
    lateinit var wifiScanner: WifiScanner

    @Inject
    lateinit var runDecisionCycleUseCase: RunDecisionCycleUseCase

    @Inject
    lateinit var decisionNotifier: DecisionNotifier

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var wifiRepository: WifiRepository

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var periodicScanJob: Job? = null

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, WifiForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, WifiForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        startInForeground()
        observeConnections()
        startPeriodicObservation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        periodicScanJob?.cancel()
        serviceScope.cancel()
        Log.d(TAG, "WifiForegroundService destroyed")
    }

    private fun startInForeground() {
        val notification = buildForegroundNotification("Active — Observing Wi-Fi networks")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
            }
            startForeground(FOREGROUND_NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationChannels.CHANNEL_ID_FOREGROUND)
            .setContentTitle("WiFi Auto Manager Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildForegroundNotification(text)
        val manager = getSystemService(NOTIFICATION_SERVICE) as? android.app.NotificationManager
        manager?.notify(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun observeConnections() {
        wifiConnectionMonitor.connectionState.onEach { state ->
            when (state) {
                is WifiState.Connected -> {
                    updateNotification("Connected to ${state.ssid} (${state.rssi} dBm)")
                    evaluateDecision()
                }
                WifiState.Disconnected -> {
                    updateNotification("Disconnected from Wi-Fi")
                    evaluateDecision()
                }
                else -> Unit
            }
        }.launchIn(serviceScope)
    }

    private fun startPeriodicObservation() {
        periodicScanJob?.cancel()
        periodicScanJob = serviceScope.launch {
            while (isActive) {
                val settings = settingsRepository.getSettings().firstOrNull()
                val intervalSec = settings?.scanIntervalSeconds?.coerceAtLeast(15) ?: 30
                delay(intervalSec * 1000L)

                // Request Wi-Fi scan and evaluate
                wifiScanner.requestScan()
                delay(2000L) // Wait for results
                evaluateDecision()
            }
        }
    }

    private suspend fun evaluateDecision() {
        val settings = settingsRepository.getSettings().firstOrNull() ?: return
        if (!settings.autoManagerEnabled) return

        val currentState = wifiConnectionMonitor.connectionState.value
        val scanState = wifiScanner.scanState.value
        val candidateScans = if (scanState is ScanState.Results) scanState.networks else emptyList()

        val decision = runDecisionCycleUseCase(currentState, candidateScans)
        if (decision.action is DecisionAction.SuggestNetwork) {
            decisionNotifier.notifyDecision(decision)
        }
    }
}
