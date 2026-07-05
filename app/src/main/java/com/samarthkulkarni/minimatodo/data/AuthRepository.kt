package com.samarthkulkarni.minimatodo.data

import android.content.Intent
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Wraps all Supabase Auth calls used by the app. Mandatory sign-in gate lives here:
 * MainActivity reads [sessionStatus] to decide whether to show AuthScreen or the task list.
 */
class AuthRepository {

    private val auth = SupabaseClientProvider.auth
    private val client = SupabaseClientProvider.client

    /** Emits Authenticated / NotAuthenticated / RefreshFailure / Initializing as the session changes. */
    val sessionStatus: StateFlow<SessionStatus> = auth.sessionStatus

    val currentUserId: String? get() = auth.currentUserOrNull()?.id

    fun currentUser(): UserInfo? = auth.currentUserOrNull()

    /** A user is "new" if their account was created within a few seconds of this sign-in. */
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun isNewAccount(): Boolean {
        val user = currentUser()
        val created = user?.createdAt ?: return false
        val lastSignIn = user.lastSignInAt ?: return true
        return (lastSignIn - created).inWholeSeconds < 5
    }

    suspend fun signUpWithEmail(email: String, password: String) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signInWithEmail(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Opens the system browser (Custom Tab) for Google OAuth; result comes back via deep link. */
    suspend fun signInWithGoogle() {
        auth.signInWith(Google)
    }

    /** Call from MainActivity.onCreate/onNewIntent so supabase-kt can complete the OAuth deep link. */
    suspend fun handleDeeplink(intent: Intent) {
        client.handleDeeplinks(intent)
    }

    suspend fun sendPasswordReset(email: String) {
        auth.resetPasswordForEmail(email)
    }

    suspend fun signOut() {
        auth.signOut()
    }
}
