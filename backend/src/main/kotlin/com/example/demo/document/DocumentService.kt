package com.example.demo.document

import com.example.demo.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import tools.jackson.databind.ObjectMapper

import com.example.demo.user.User
import org.springframework.security.core.Authentication

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val userRepository: UserRepository,
    private val pythonAnalyzerClient: PythonAnalyzerClient,
    private val objectMapper: ObjectMapper
) {

    private val storageDir = run {
        val currentDir = Paths.get("").toAbsolutePath()
        val dir = if (currentDir.endsWith("backend")) {
            currentDir.parent.resolve("samples").resolve("pdf")
        } else {
            currentDir.resolve("samples").resolve("pdf")
        }
        if (!Files.exists(dir)) {
            Files.createDirectories(dir)
        }
        dir
    }

    fun uploadAndProcess(file: MultipartFile, userEmail: String): Document {
        // Find user
        val user = userRepository.findByEmail(userEmail)
            ?: throw IllegalArgumentException("User not found with email: $userEmail")

        // Save file to samples/pdf
        val originalFilename = file.originalFilename ?: "document.pdf"
        val uniqueFilename = "${UUID.randomUUID()}_$originalFilename"
        val targetPath = storageDir.resolve(uniqueFilename)
        Files.write(targetPath, file.bytes)

        // Call python analyzer (FastAPI) to process document
        var extractedJson: String? = null
        try {
            extractedJson = pythonAnalyzerClient.analyze(file.bytes, originalFilename)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Parse type from JSON response by extracting the "document_type" key from "extracted_data"
        val type = try {
            val json = extractedJson
            if (json.isNullOrEmpty()) {
                "unknown"
            } else {
                val map = objectMapper.readValue(json, Map::class.java) as Map<*, *>
                val extractedData = map["extracted_data"] as? Map<*, *>
                extractedData?.get("document_type")?.toString() ?: "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }

        // Save metadata to DB
        val document = Document(
            fileName = originalFilename,
            filePath = targetPath.toAbsolutePath().toString(),
            type = type,
            user = user,
            extractedJson = extractedJson
        )

        return documentRepository.save(document)
    }

    private fun getAccessibleDocuments(userEmail: String): List<Document> {
        val user = userRepository.findByEmail(userEmail)
            ?: throw IllegalArgumentException("User not found with email: $userEmail")
        return if (user.role.name == "ADMIN") {
            documentRepository.findAll()
        } else {
            documentRepository.findByUserEmail(userEmail)
        }
    }

    private fun verifyDocumentAccess(document: Document, user: User) {
        if (document.user.id != user.id && user.role.name != "ADMIN") {
            throw SecurityException("Unauthorized to access this document")
        }
    }

    fun deleteDocument(id: Long, authentication: Authentication) {
        val document = documentRepository.findById(id)
            .orElseThrow { NoSuchElementException("Document not found with ID: $id") }

        val currentUser = authentication.principal as? User
            ?: throw SecurityException("Invalid authentication principal")

        verifyDocumentAccess(document, currentUser)
        documentRepository.delete(document)
    }

    fun getUserDocuments(userEmail: String): List<Document> {
        return getAccessibleDocuments(userEmail)
    }

    fun getDocumentById(id: Long, userEmail: String): Document {
        val doc = documentRepository.findById(id)
            .orElseThrow { NoSuchElementException("Document not found with ID: $id") }

        val user = userRepository.findByEmail(userEmail)
            ?: throw IllegalArgumentException("User not found with email: $userEmail")

        verifyDocumentAccess(doc, user)
        return doc
    }

    fun getDocumentFile(id: Long, userEmail: String): File {
        val doc = getDocumentById(id, userEmail)
        val file = File(doc.filePath)
        if (!file.exists()) {
            throw NoSuchElementException("Physical file not found on disk")
        }
        return file
    }

    fun getDistinctTypesByUser(userEmail: String): List<String> {
        val documents = getUserDocuments(userEmail)
        return documents.map { it.type }.distinct()
    }
}
