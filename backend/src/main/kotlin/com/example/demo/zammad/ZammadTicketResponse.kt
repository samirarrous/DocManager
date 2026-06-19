package com.example.demo.zammad

import com.fasterxml.jackson.annotation.JsonProperty

data class ZammadTicketResponse(
    val id: Long,
    val number: String,
    val title: String,
    @JsonProperty("customer_id") val customerId: Long,
    @JsonProperty("group_id") val groupId: Long,
    @JsonProperty("state_id") val stateId: Int,
    @JsonProperty("created_at") val createdAt: String,
    @JsonProperty("updated_at") val updatedAt: String
)
