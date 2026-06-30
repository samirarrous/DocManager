package com.example.demo.authentification

import com.example.demo.user.User
import com.example.demo.user.UserRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val userRepository: UserRepository
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterDTO): User {
        return authService.register(request)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LogInDTO): AuthResponse {
        return authService.login(request)
    }

    @GetMapping("/me")
    fun me(request: HttpServletRequest): User {
        val email = request.getAttribute("currentUserEmail") as? String
            ?: throw org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Non authentifié"
            )
        return userRepository.findByEmail(email)
            ?: throw org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Utilisateur introuvable"
            )
    }
}