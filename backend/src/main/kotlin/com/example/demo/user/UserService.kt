package com.example.demo.user

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun createUser(user: User): User {
        if (userRepository.findByEmail(user.email) != null) {
            throw IllegalArgumentException("Email already in use: ${user.email}")
        }

        val hashedPassword = passwordEncoder.encode(user.password)!!
        val userToSave = User(
            email = user.email,
            password = hashedPassword,
            role = user.role
        )
        return userRepository.save(userToSave)
    }

    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    fun findAll(): List<User> {
        return userRepository.findAll()
    }
}
