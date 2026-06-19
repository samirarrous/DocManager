package com.example.demo.zammad

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class ZammadService(
    @Value("\${zammad.api.url}") private val baseUrl: String,
    @Value("\${zammad.api.token}") private val token: String
) {

    private val restTemplate = RestTemplate()


    fun createTicket(title: String, description: String, customerEmail: String): ZammadTicketResponse {

        val url = "$baseUrl/tickets"

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("Authorization", "Token token=$token")

        val body = mapOf(
            "title" to title,
            "group" to "Users",
            "customer_id" to "guess:$customerEmail",
            "article" to mapOf(
                "subject" to title,
                "body" to description,
                "type" to "web",
                "internal" to false
            )
        )

        val request = HttpEntity(body, headers)

        return restTemplate.postForObject(url, request, ZammadTicketResponse::class.java)
            ?: throw IllegalStateException("Zammad response was empty")
    }

    fun getTicketsByCustomer(email: String): List<ZammadTicketResponse> {
        val url = "$baseUrl/tickets/search?query=customer.email:$email&expand=true"
        val headers = HttpHeaders()
        headers.set("Authorization", "Token token=$token")
        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            url,
            org.springframework.http.HttpMethod.GET,
            request,
            Array<ZammadTicketResponse>::class.java
        )
        return response.body?.toList() ?: emptyList()
    }

    fun getFirstArticleBody(ticketId: Long): String {
        try {
            val url = "$baseUrl/ticket_articles/by_ticket/$ticketId"
            val headers = HttpHeaders()
            headers.set("Authorization", "Token token=$token")
            val request = HttpEntity<Void>(headers)

            val response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                request,
                List::class.java
            )
            val articles = response.body as? List<*>
            if (!articles.isNullOrEmpty()) {
                val firstArticle = articles[0] as? Map<*, *>
                return firstArticle?.get("body") as? String ?: ""
            }
        } catch (e: Exception) {
            // Fallback if articles cannot be loaded
        }
        return ""
    }
}