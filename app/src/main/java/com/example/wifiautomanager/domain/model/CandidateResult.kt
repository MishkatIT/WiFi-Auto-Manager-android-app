package com.example.wifiautomanager.domain.model

data class CandidateResult(
    val network: WifiNetwork,
    val scannedNetwork: ScannedNetwork?,
    val qualified: Boolean,
    val rejectionReason: String?,
    val signalImprovement: Int?
)
