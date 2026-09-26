package com.example.wifiautomanager.domain.model

sealed class SuggestionState {
    data object NotRegistered : SuggestionState()
    data object Registered : SuggestionState()
    data object Duplicate : SuggestionState()
    data object NotAllowed : SuggestionState()
    data class Failed(val reason: String) : SuggestionState()
    data object ApiNotSupported : SuggestionState()

    val label: String
        get() = when (this) {
            is NotRegistered -> "Not Suggested"
            is Registered -> "Suggested (Active)"
            is Duplicate -> "Duplicate Suggestion"
            is NotAllowed -> "Not Allowed by OS"
            is Failed -> "Failed: $reason"
            is ApiNotSupported -> "Requires Android 10+"
        }

    val isSuccess: Boolean
        get() = this is Registered
}
