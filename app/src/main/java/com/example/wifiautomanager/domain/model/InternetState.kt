package com.example.wifiautomanager.domain.model

data class InternetState(
    val status: InternetStatus,
    val unavailableSinceMs: Long? = null
)
