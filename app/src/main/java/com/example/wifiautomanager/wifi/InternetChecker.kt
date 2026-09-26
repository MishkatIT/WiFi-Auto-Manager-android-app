package com.example.wifiautomanager.wifi

import com.example.wifiautomanager.domain.model.InternetStatus

// InternetChecker validates active internet capability - implemented in Phase 4
class InternetChecker {
    fun checkInternet(): InternetStatus = InternetStatus.UNKNOWN
}
