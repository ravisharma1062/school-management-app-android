package com.school.app.viewmodel

import com.school.app.data.Outcome

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Ready<T>(val data: T, val fromCache: Boolean = false) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun <T> Outcome<T>.toUiState(): UiState<T> = when (this) {
    is Outcome.Success -> UiState.Ready(data, fromCache)
    is Outcome.Failure -> UiState.Error(message)
}
