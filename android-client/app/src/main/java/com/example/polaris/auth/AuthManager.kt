package com.example.polaris.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.gson.Gson

class AuthManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    companion object {
        private const val PREFS_NAME = "polaris_auth"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "user_data"
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"

        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        loadStoredAuth()
    }

    private fun loadStoredAuth() {
        val isAuthenticated = prefs.getBoolean(KEY_IS_AUTHENTICATED, false)
        val token = prefs.getString(KEY_TOKEN, null)
        val userJson = prefs.getString(KEY_USER, null)
        val user = userJson?.let {
            try {
                gson.fromJson(it, User::class.java)
            } catch (e: Exception) {
                null
            }
        }

        _authState.value = AuthState(
            isAuthenticated = isAuthenticated && token != null,
            token = token,
            user = user
        )
    }

    fun setLoading(loading: Boolean) {
        _authState.value = _authState.value.copy(isLoading = loading, error = null)
    }

    fun setError(error: String) {
        _authState.value = _authState.value.copy(isLoading = false, error = error)
    }

    fun login(token: String) {
        _authState.value = AuthState(
            isAuthenticated = true,
            token = token,
            isLoading = false,
            error = null
        )

        // Save to persistent storage
        prefs.edit().apply {
            putBoolean(KEY_IS_AUTHENTICATED, true)
            putString(KEY_TOKEN, token)
            apply()
        }
    }

    fun logout() {
        _authState.value = AuthState()

        // Clear persistent storage
        prefs.edit().clear().apply()
    }

    fun getToken(): String? = _authState.value.token

    fun isAuthenticated(): Boolean = _authState.value.isAuthenticated

    fun getCurrentUser(): User? = _authState.value.user
}