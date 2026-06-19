package com.example.demo.authentification

import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class SessionManager {
    // Maps token (UUID string) -> user email
    private val sessions = ConcurrentHashMap<String, String>()

    fun createSession(email: String): String {
        val token = UUID.randomUUID().toString()
        sessions[token] = email
        return token
    }

    fun getEmailByToken(token: String): String? {
        return sessions[token]
    }

    fun invalidateSession(token: String) {
        sessions.remove(token)
    }
}
