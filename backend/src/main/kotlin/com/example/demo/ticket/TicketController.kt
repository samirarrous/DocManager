package com.example.demo.ticket

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/tickets")
class TicketController(
    private val ticketService: TicketService
) {

    @PostMapping
    fun create(@RequestBody request: CreateTicketRequest): TicketDto {
        return ticketService.createTicket(request)
    }

    @GetMapping
    fun getByEmail(@RequestParam email: String): List<TicketDto> {
        return ticketService.getTicketsByEmail(email)
    }
}