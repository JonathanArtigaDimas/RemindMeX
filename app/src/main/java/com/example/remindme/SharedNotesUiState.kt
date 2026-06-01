package com.example.remindme

sealed interface SharedNotesUiState {
    object Loading : SharedNotesUiState
    object Success : SharedNotesUiState
    data class Error(val message: String) : SharedNotesUiState
    object Idle : SharedNotesUiState
}
