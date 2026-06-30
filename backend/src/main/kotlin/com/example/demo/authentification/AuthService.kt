package com.example.demo.authentification

import com.example.demo.user.User
import com.example.demo.user.UserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userService: UserService,
    private val sessionManager: SessionManager,
    private val passwordEncoder: PasswordEncoder
) {

    fun register(request: RegisterDTO): User {
        val user = User(
            email = request.email,
            password = request.password
        )
        return userService.createUser(user)
    }

    fun login(request: LogInDTO): AuthResponse {
        val user = userService.findByEmail(request.email)
            ?: throw org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Utilisateur introuvable"
            )

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Mot de passe incorrect"
            )
        }

        val token = sessionManager.createSession(user)
        return AuthResponse(token, user)
    }
}