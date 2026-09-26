package com.example.wifiautomanager.domain.decision

import com.example.wifiautomanager.domain.model.CandidateResult
import com.example.wifiautomanager.domain.model.ScannedNetwork
import com.example.wifiautomanager.domain.model.WifiNetwork

class CandidateSelector {
    fun selectCandidates(
        savedNetworks: List<WifiNetwork>,
        scannedNetworks: List<ScannedNetwork>
    ): List<CandidateResult> {
        return emptyList()
    }
}
