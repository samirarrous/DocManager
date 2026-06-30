package com.example.demo.authentification

import com.example.demo.user.User
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class SessionManager {
    // Maps token (UUID string) -> User object
    private val sessions = ConcurrentHashMap<String, User>()

    fun createSession(user: User): String {
        val token = UUID.randomUUID().toString()
        sessions[token] = user
        return token
    }

    fun getUserByToken(token: String): User? {
        return sessions[token]
    }

    fun getEmailByToken(token: String): String? {
        return sessions[token]?.email
    }

    fun invalidateSession(token: String) {
        sessions.remove(token)
    }
}
