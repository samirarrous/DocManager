package com.example.demo.authentification

import com.example.demo.user.User
import com.example.demo.user.UserService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    lateinit var userService: UserService

    @Mock
    lateinit var sessionManager: SessionManager

    @Mock
    lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    lateinit var authService: AuthService

    @Test
    fun `register creates and returns a user`() {
        val request = RegisterDTO(email = "test@example.com", password = "password123")
        val expectedUser = User(email = request.email, password = "encodedPassword")

        `when`(userService.createUser(anyUser())).thenReturn(expectedUser)

        val result = authService.register(request)

        assertNotNull(result)
        assertEquals(expectedUser.email, result.email)
    }

    @Test
    fun `login returns auth response with token on valid credentials`() {
        val request = LogInDTO(email = "test@example.com", password = "password123")
        val user = User(email = request.email, password = "encodedPassword")
        val expectedToken = "valid-jwt-token"

        `when`(userService.findByEmail(request.email)).thenReturn(user)
        `when`(passwordEncoder.matches(request.password, user.password)).thenReturn(true)
        `when`(sessionManager.createSession(request.email)).thenReturn(expectedToken)

        val result = authService.login(request)

        assertNotNull(result)
        assertEquals(expectedToken, result.token)
        assertEquals(user.email, result.user.email)
    }

    @Test
    fun `login throws exception when user is not found`() {
        val request = LogInDTO(email = "nonexistent@example.com", password = "password123")

        `when`(userService.findByEmail(request.email)).thenReturn(null)

        val exception = assertThrows(org.springframework.web.server.ResponseStatusException::class.java) {
            authService.login(request)
        }

        assertEquals(org.springframework.http.HttpStatus.UNAUTHORIZED, exception.statusCode)
        assertEquals("Utilisateur introuvable", exception.reason)
    }

    @Test
    fun `login throws exception on incorrect password`() {
        val request = LogInDTO(email = "test@example.com", password = "wrongpassword")
        val user = User(email = request.email, password = "encodedPassword")

        `when`(userService.findByEmail(request.email)).thenReturn(user)
        `when`(passwordEncoder.matches(request.password, user.password)).thenReturn(false)

        val exception = assertThrows(org.springframework.web.server.ResponseStatusException::class.java) {
            authService.login(request)
        }

        assertEquals(org.springframework.http.HttpStatus.UNAUTHORIZED, exception.statusCode)
        assertEquals("Mot de passe incorrect", exception.reason)
    }

    // Helper method because Mockito.any() can return null, causing issues in Kotlin non-null params
    private fun anyUser(): User {
        return org.mockito.Mockito.any(User::class.java) ?: User(email = "", password = "")
    }
}
