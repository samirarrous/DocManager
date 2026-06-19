package com.example.demo.ticket

data class TicketDto(
    val id: Long,
    val zammadId: Long,
    val number: String,
    val title: String,
    val description: String,
    val status: String,
    val user: UserDto
)

data class UserDto(
    val email: String
)
