package com.example.wifiautomanager.domain.model

data class AppSettings(
    val autoManagerEnabled: Boolean = false,
    val scanIntervalSeconds: Int = 30,
    val internetCheckIntervalSeconds: Int = 10,
    val internetUnavailableTimeoutSeconds: Int = 15,
    val switchCooldownSeconds: Int = 60,
    val stabilityWindowSeconds: Int = 10,
    val showDecisionNotifications: Boolean = true,
    val logRetentionDays: Int = 7,
)
