package com.example.wifiautomanager.domain.usecase

import com.example.wifiautomanager.domain.model.InternetStatus

class CheckInternetUseCase {
    operator fun invoke(): InternetStatus {
        return InternetStatus.UNKNOWN
    }
}
