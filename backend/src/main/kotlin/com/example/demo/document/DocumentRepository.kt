package com.example.demo.document

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentRepository : JpaRepository<Document, Long> {
    fun findByUserEmail(email: String): List<Document>
}
