package com.samarthkulkarni.minimatodo.auth

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samarthkulkarni.minimatodo.data.AuthRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Emitted right after a successful sign-in/sign-up so the UI can show the welcome animation. */
data class AuthSuccess(val isNewAccount: Boolean)

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val authSuccess: AuthSuccess? = null
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

    /**
     * Single entry point for email auth. Tries signing in first; if that fails
     * (most likely because no account exists yet with this email), attempts to
     * create one. There's no separate "sign up" mode - the user just enters
     * their details and continues.
     */
    fun submitEmailAuth() {
        val email = uiState.email.trim()
        val password = uiState.password

        if (email.isEmpty() || password.isEmpty()) {
            uiState = uiState.copy(errorMessage = "Enter email and password")
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = null, infoMessage = null)
        viewModelScope.launch {
            try {
                repository.signInWithEmail(email, password)
                uiState = uiState.copy(authSuccess = AuthSuccess(isNewAccount = false))
            } catch (signInError: Exception) {
                if (password.length < 6) {
                    uiState = uiState.copy(errorMessage = "Password must be at least 6 characters")
                    uiState = uiState.copy(isLoading = false)
                    return@launch
                }
                try {
                    repository.signUpWithEmail(email, password)
                    if (repository.currentUserId != null) {
                        uiState = uiState.copy(authSuccess = AuthSuccess(isNewAccount = true))
                    } else {
                        // Project has email confirmation enabled - no session yet.
                        uiState = uiState.copy(
                            infoMessage = "Check your email to confirm your account, then continue"
                        )
                    }
                } catch (signUpError: Exception) {
                    val msg = signUpError.message ?: ""
                    if (msg.contains("already registered", ignoreCase = true) ||
                        msg.contains("already exists", ignoreCase = true)) {
                        uiState = uiState.copy(errorMessage = "Incorrect password")
                    } else {
                        uiState = uiState.copy(errorMessage = "Sign-up failed: $msg")
                    }
                }
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

    /** Called from MainActivity once the browser OAuth redirect lands back in the app. */
    fun handleDeeplink(intent: Intent) {
        viewModelScope.launch {
            try {
                repository.handleDeeplink(intent)
                if (repository.currentUserId != null) {
                    uiState = uiState.copy(authSuccess = AuthSuccess(isNewAccount = repository.isNewAccount()))
                }
            } catch (e: Exception) {
                uiState = uiState.copy(errorMessage = e.message ?: "Sign-in failed")
            }
        }
    }

    /** Called once the welcome animation finishes playing. */
    fun completeAuthFlow() {
        uiState = uiState.copy(authSuccess = null)
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }
}
