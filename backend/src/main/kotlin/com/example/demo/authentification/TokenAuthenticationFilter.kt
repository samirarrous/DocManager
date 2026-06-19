package com.example.demo.authentification

import com.example.demo.user.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TokenAuthenticationFilter(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            val email = sessionManager.getEmailByToken(token)
            if (email != null) {
                val user = userRepository.findByEmail(email)
                if (user != null) {
                    val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
                    val authentication = UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        authorities
                    )
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication

                    // Retain this attribute for controller compatibility
                    request.setAttribute("currentUserEmail", email)
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}
