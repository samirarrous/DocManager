package com.example.demo.ticket

data class CreateTicketRequest(
    val title: String,
    val description: String,
    val email: String
)
