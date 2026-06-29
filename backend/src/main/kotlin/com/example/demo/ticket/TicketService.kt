package com.example.demo.ticket

import com.example.demo.user.UserRepository
import com.example.demo.zammad.ZammadService
import org.springframework.stereotype.Service


/**
 * Service handling integration with the Zammad ticketing system.
 * 
 * It manages support ticket creation and customer ticket history retrieval.
 * Outages of the external Zammad service are bubbled as 503 SERVICE_UNAVAILABLE.
 */
@Service
class TicketService(
    private val userRepository: UserRepository,
    private val zammadService: ZammadService
) {

    fun createTicket(request: CreateTicketRequest): TicketDto {
        // Verify user exists in our local DB
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("User not found with email: ${request.email}")

        return try {
            val zammadResponse = zammadService.createTicket(
                request.title,
                request.description,
                request.email
            )

            TicketDto(
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
        } catch (e: Exception) {
            e.printStackTrace()
            throw org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "Zammad is unreachable",
                e
            )
        }
    }

    /**
     * Retrieves the list of support tickets open for a customer by their email.
     * 
     * @param email The email address of the customer.
     * @return The list of tickets mapped to TicketDto.
     * @throws ResponseStatusException with 503 status if the external Zammad server is unreachable.
     */
    fun getTicketsByEmail(email: String): List<TicketDto> {
        return try {
            val zammadTickets = zammadService.getTicketsByCustomer(email)
            zammadTickets.map { ticket ->
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
        } catch (e: Exception) {
            e.printStackTrace()
            throw org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "Zammad is unreachable",
                e
            )
        }
    }
}