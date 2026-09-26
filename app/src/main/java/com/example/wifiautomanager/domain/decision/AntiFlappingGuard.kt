package com.example.wifiautomanager.domain.decision

sealed class AntiFlappingResult {
    data class Allowed(val improvementDbm: Int) : AntiFlappingResult()
    data class Blocked(val reason: String) : AntiFlappingResult()
}

class AntiFlappingGuard {
    private var lastSwitchTimestampMs: Long = 0L

    fun shouldAllowSwitch(
        currentRssi: Int,
        candidateRssi: Int,
        requiredImprovementDbm: Int,
        lastSwitchTimeMs: Long?,
        cooldownSeconds: Int,
        currentTimeMs: Long
    ): AntiFlappingResult {
        // Cooldown check
        val switchTime = lastSwitchTimeMs ?: (if (lastSwitchTimestampMs > 0) lastSwitchTimestampMs else null)
        if (switchTime != null) {
            val elapsedMs = currentTimeMs - switchTime
            val cooldownMs = cooldownSeconds * 1000L
            if (elapsedMs < cooldownMs) {
                val remainingSec = ((cooldownMs - elapsedMs) / 1000L).coerceAtLeast(1)
                return AntiFlappingResult.Blocked("Cooldown active: ${remainingSec}s remaining")
            }
        }

        // Improvement check
        val improvement = candidateRssi - currentRssi
        if (improvement < requiredImprovementDbm) {
            return AntiFlappingResult.Blocked(
                "Improvement ${improvement} dBm < required ${requiredImprovementDbm} dBm"
            )
        }

        return AntiFlappingResult.Allowed(improvementDbm = improvement)
    }

    fun canSwitch(cooldownSeconds: Int): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastSwitchTimestampMs) >= (cooldownSeconds * 1000L)
    }

    fun recordSwitch(timestampMs: Long = System.currentTimeMillis()) {
        lastSwitchTimestampMs = timestampMs
    }

    fun getRemainingCooldownSeconds(cooldownSeconds: Int): Int {
        val elapsed = (System.currentTimeMillis() - lastSwitchTimestampMs) / 1000L
        val remaining = cooldownSeconds - elapsed
        return if (remaining > 0) remaining.toInt() else 0
    }
}
