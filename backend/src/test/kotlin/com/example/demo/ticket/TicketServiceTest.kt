package com.example.demo.ticket

import com.example.demo.user.UserRepository
import com.example.demo.zammad.ZammadService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@ExtendWith(MockitoExtension::class)
class TicketServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var zammadService: ZammadService

    @InjectMocks
    lateinit var ticketService: TicketService

    @Test
    fun `getTicketsByEmail throws 503 SERVICE_UNAVAILABLE when Zammad throws an exception`() {
        val email = "test@example.com"
        
        // Simuler une coupure de connexion ou exception levée par Zammad
        `when`(zammadService.getTicketsByCustomer(email))
            .thenThrow(RuntimeException("Connection refused"))

        // Vérifier que l'exception ResponseStatusException (503) est bien levée
        val exception = assertThrows(ResponseStatusException::class.java) {
            ticketService.getTicketsByEmail(email)
        }

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.statusCode)
        assertEquals("Zammad is unreachable", exception.reason)
    }
}
