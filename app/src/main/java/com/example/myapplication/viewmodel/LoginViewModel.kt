package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val msg: String) : LoginUiState()
}

class LoginViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(user: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = repository.login(user, pass)
            _uiState.value =
                if (result.isSuccess) LoginUiState.Success
                else LoginUiState.Error("Credenciales inválidas")
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = LoginUiState.Idle
        }
    }
}
