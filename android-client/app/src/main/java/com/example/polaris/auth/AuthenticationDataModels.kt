package com.example.polaris.auth

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
//    val success: Boolean,
    val token: String?,
//    val message: String?,
//    val user: User?
)

data class User(
    val id: String,
    val username: String,
    val email: String?
)

data class AuthState(
    val isAuthenticated: Boolean = false,
    val token: String? = null,
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)