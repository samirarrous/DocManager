package com.example.demo.authentification

import com.example.demo.user.User

data class AuthResponse(
    val token: String,
    val user: User
)
