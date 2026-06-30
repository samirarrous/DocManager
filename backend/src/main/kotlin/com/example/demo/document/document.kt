package com.example.demo.document

import com.example.demo.user.User
import jakarta.persistence.*

@Entity
@Table(name = "documents",
    indexes = [Index(name = "idx_documents_user_id", columnList = "user_id")]
)
class Document(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val fileName: String,

    val filePath: String,

    val type: String,

    @ManyToOne(fetch = FetchType.LAZY)
    val user: User,

    @Column(columnDefinition = "TEXT")
    var extractedJson: String? = null
)