package com.example.demo.zammad

import com.fasterxml.jackson.annotation.JsonProperty

data class ZammadSearchResponse(
    val tickets: List<ZammadTicketResponse>,
    @JsonProperty("tickets_count") val ticketsCount: Int
)
