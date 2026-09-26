package com.example.wifiautomanager.domain.rule

import com.example.wifiautomanager.domain.decision.EvaluationContext
import com.example.wifiautomanager.domain.model.InternetStatus
import com.example.wifiautomanager.domain.model.ScannedNetwork
import com.example.wifiautomanager.domain.model.WifiNetwork
import com.example.wifiautomanager.domain.model.WifiState
import java.util.Calendar

class ConditionEvaluator {

    fun evaluate(
        condition: Condition,
        context: EvaluationContext,
        currentState: WifiState,
        candidate: ScannedNetwork? = null,
        candidateSaved: WifiNetwork? = null,
        savedNetworks: List<WifiNetwork> = emptyList()
    ): Boolean = when (condition) {
        is Condition.CurrentSignalBelow -> {
            val rssi = (currentState as? WifiState.Connected)?.rssi ?: return false
            rssi < condition.thresholdDbm
        }

        is Condition.CurrentSignalAbove -> {
            val rssi = (currentState as? WifiState.Connected)?.rssi ?: return false
            rssi > condition.thresholdDbm
        }

        is Condition.CandidateSignalAbove -> {
            val rssi = candidate?.rssi ?: return false
            rssi > condition.thresholdDbm
        }

        is Condition.SignalImprovementAtLeast -> {
            val currentRssi = (currentState as? WifiState.Connected)?.rssi ?: -127
            val candidateRssi = candidate?.rssi ?: return false
            (candidateRssi - currentRssi) >= condition.improvementDbm
        }

        is Condition.InternetAvailable -> {
            val status = (currentState as? WifiState.Connected)?.internetStatus
            status == InternetStatus.AVAILABLE
        }

        is Condition.InternetUnavailable -> {
            val status = (currentState as? WifiState.Connected)?.internetStatus
            status == InternetStatus.UNAVAILABLE
        }

        is Condition.InternetUnavailableForAtLeast -> {
            val since = context.internetUnavailableSinceMs ?: return false
            val elapsedMs = context.currentTimeMs - since
            elapsedMs >= (condition.seconds * 1000L)
        }

        is Condition.CurrentSsidIs -> {
            val currentSsid = (currentState as? WifiState.Connected)?.ssid ?: return false
            currentSsid.equals(condition.ssid, ignoreCase = true)
        }

        is Condition.CurrentSsidIsNot -> {
            val currentSsid = (currentState as? WifiState.Connected)?.ssid ?: return true
            !currentSsid.equals(condition.ssid, ignoreCase = true)
        }

        is Condition.CandidateNetworkAvailable -> {
            candidateSaved?.id == condition.networkId
        }

        is Condition.TimeBetween -> {
            val cal = Calendar.getInstance().apply {
                timeInMillis = context.currentTimeMs
            }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val min = cal.get(Calendar.MINUTE)
            val current = hour * 60 + min
            val start = condition.startHour * 60 + condition.startMin
            val end = condition.endHour * 60 + condition.endMin

            if (start <= end) {
                current in start..end
            } else {
                current >= start || current <= end // crosses midnight
            }
        }

        is Condition.DayOfWeek -> {
            val cal = Calendar.getInstance().apply {
                timeInMillis = context.currentTimeMs
            }
            val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> java.time.DayOfWeek.MONDAY
                Calendar.TUESDAY -> java.time.DayOfWeek.TUESDAY
                Calendar.WEDNESDAY -> java.time.DayOfWeek.WEDNESDAY
                Calendar.THURSDAY -> java.time.DayOfWeek.THURSDAY
                Calendar.FRIDAY -> java.time.DayOfWeek.FRIDAY
                Calendar.SATURDAY -> java.time.DayOfWeek.SATURDAY
                else -> java.time.DayOfWeek.SUNDAY
            }
            condition.days.contains(dayOfWeek)
        }

        is Condition.ConnectedForAtLeast -> {
            val connectedSince = context.connectedSinceMs ?: return false
            val elapsed = context.currentTimeMs - connectedSince
            elapsed >= (condition.seconds * 1000L)
        }

        is Condition.SwitchCooldownExpired -> {
            val lastSwitch = context.lastSwitchTimeMs ?: return true
            val elapsed = context.currentTimeMs - lastSwitch
            elapsed >= (condition.seconds * 1000L)
        }

        is Condition.CandidatePriorityHigherThan -> {
            val targetNet = savedNetworks.find { it.id == condition.networkId } ?: return false
            val candidatePriority = candidateSaved?.priority ?: return false
            candidatePriority > targetNet.priority
        }
    }
}
