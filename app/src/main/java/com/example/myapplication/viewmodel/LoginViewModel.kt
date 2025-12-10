package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.User
import com.example.myapplication.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val user: User) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun checkExistingSession() {
        viewModelScope.launch {
            val logged = userRepository.isLoggedIn()
            if (logged) {
                val u = userRepository.getCurrentUser()
                if (u != null) _uiState.value = LoginUiState.Success(u)
            }
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Usuario y contraseña son requeridos")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val res = userRepository.login(username.trim(), password)
            res.onSuccess { user ->
                _uiState.value = LoginUiState.Success(user)
            }.onFailure { throwable ->
                _uiState.value = LoginUiState.Error(throwable.message ?: "Error en autenticación")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            _uiState.value = LoginUiState.Idle
        }
    }
}
