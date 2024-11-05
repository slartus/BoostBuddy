package ru.slartus.boostbuddy.components.common

sealed class ProgressState {
    data object Init : ProgressState()
    data object Loading : ProgressState()
    data object Loaded : ProgressState()
    data class Error(val description: String) : ProgressState()
}