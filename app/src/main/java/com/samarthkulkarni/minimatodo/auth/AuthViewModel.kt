package com.samarthkulkarni.minimatodo.auth

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samarthkulkarni.minimatodo.data.AuthRepository
import io.github.jan.supabase.auth.SessionStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class AuthMode { SIGN_IN, SIGN_UP }

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()

    val sessionStatus: StateFlow<SessionStatus> = repository.sessionStatus

    var uiState by mutableStateOf(AuthUiState())
        private set

    fun onEmailChange(value: String) {
        uiState = uiState.copy(email = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value, errorMessage = null)
    }

    fun toggleMode() {
        uiState = uiState.copy(
            mode = if (uiState.mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN,
            errorMessage = null,
            infoMessage = null
        )
    }

    fun submitEmailAuth() {
        val email = uiState.email.trim()
        val password = uiState.password

        if (email.isEmpty() || password.isEmpty()) {
            uiState = uiState.copy(errorMessage = "Enter email and password")
            return
        }
        if (password.length < 6) {
            uiState = uiState.copy(errorMessage = "Password must be at least 6 characters")
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = null, infoMessage = null)
        viewModelScope.launch {
            try {
                if (uiState.mode == AuthMode.SIGN_IN) {
                    repository.signInWithEmail(email, password)
                } else {
                    repository.signUpWithEmail(email, password)
                    uiState = uiState.copy(
                        infoMessage = "Check your email to confirm your account, then sign in."
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(errorMessage = e.message ?: "Something went wrong")
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    fun signInWithGoogle() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                repository.signInWithGoogle()
            } catch (e: Exception) {
                uiState = uiState.copy(errorMessage = e.message ?: "Google sign-in failed")
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    fun handleDeeplink(intent: Intent) {
        viewModelScope.launch {
            try {
                repository.handleDeeplink(intent)
            } catch (e: Exception) {
                uiState = uiState.copy(errorMessage = e.message ?: "Sign-in failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }
}
