package com.example.demo.authentification

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterDTO(
    @field:NotBlank(message = "L'email est requis")
    @field:Email(message = "L'email doit être valide")
    val email: String,
    @field:NotBlank(message = "Le mot de passe est requis")
    @field:Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    val password: String
)
