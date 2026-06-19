package com.example.demo.document

import com.example.demo.user.User
import jakarta.persistence.*

@Entity
@Table(name = "documents")
class Document(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val fileName: String,

    val filePath: String,

    val type: String,

    @ManyToOne
    val user: User,

    @Column(columnDefinition = "TEXT")
    var extractedJson: String? = null
)