package com.example.wifiautomanager.domain.usecase

import com.example.wifiautomanager.domain.model.CandidateResult
import com.example.wifiautomanager.domain.model.ScannedNetwork
import com.example.wifiautomanager.domain.model.WifiNetwork

class EvaluateCandidatesUseCase {
    operator fun invoke(
        savedNetworks: List<WifiNetwork>,
        scannedNetworks: List<ScannedNetwork>
    ): List<CandidateResult> {
        return emptyList()
    }
}
