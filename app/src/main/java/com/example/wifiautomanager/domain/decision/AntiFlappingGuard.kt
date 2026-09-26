package com.example.wifiautomanager.domain.decision

class AntiFlappingGuard {
    private var lastSwitchTimestampMs: Long = 0L

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
