package com.meownit.nitweather

sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data class Success(val message: String) : UiState
    data class Error(val message: String) : UiState
    data class NavigateToPage(val page: Int) : UiState
}
