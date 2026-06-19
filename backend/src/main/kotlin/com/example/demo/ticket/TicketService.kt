package com.example.demo.ticket

import com.example.demo.user.UserRepository
import com.example.demo.zammad.ZammadService
import org.springframework.stereotype.Service


@Service
class TicketService(
    private val userRepository: UserRepository,
    private val zammadService: ZammadService
) {

    fun createTicket(request: CreateTicketRequest): TicketDto {
        // Verify user exists in our local DB
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("User not found with email: ${request.email}")

        val zammadResponse = zammadService.createTicket(
            request.title,
            request.description,
            request.email
        )

        return TicketDto(
            id = zammadResponse.id,
            zammadId = zammadResponse.id,
            number = zammadResponse.number,
            title = request.title,
            description = request.description,
            status = when (zammadResponse.stateId) {
                1 -> "NEW"
                2 -> "OPEN"
                4 -> "CLOSED"
                7 -> "PENDING CLOSE"
                else -> "OPEN"
            },
            user = UserDto(email = user.email)
        )
    }

    fun getTicketsByEmail(email: String): List<TicketDto> {
        val zammadTickets = zammadService.getTicketsByCustomer(email)
        return zammadTickets.map { ticket ->
            val description = zammadService.getFirstArticleBody(ticket.id)
            TicketDto(
                id = ticket.id,
                zammadId = ticket.id,
                number = ticket.number,
                title = ticket.title,
                description = description,
                status = when (ticket.stateId) {
                    1 -> "NEW"
                    2 -> "OPEN"
                    4 -> "CLOSED"
                    7 -> "PENDING CLOSE"
                    else -> "OPEN"
                },
                user = UserDto(email = email)
            )
        }
    }
}