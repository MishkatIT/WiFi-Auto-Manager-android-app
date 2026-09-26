package com.example.wifiautomanager.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.wifiautomanager.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BootReceiver"

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        onReceiveInternal(context, intent, isAsyncSupported = true)
    }

    internal fun onReceiveInternal(context: Context, intent: Intent, isAsyncSupported: Boolean = true) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Boot completed broadcast received")
        val pendingResult = if (isAsyncSupported) {
            try {
                goAsync()
            } catch (e: Throwable) {
                null
            }
        } else null

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = settingsRepository.getSettings().firstOrNull()
                if (settings?.autoManagerEnabled == true) {
                    Log.i(TAG, "AutoManager is enabled; scheduling background DecisionWorker on boot")
                    DecisionWorker.schedule(context)
                } else {
                    Log.d(TAG, "AutoManager is disabled; not scheduling background worker")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling boot broadcast", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
