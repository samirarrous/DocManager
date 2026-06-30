package com.example.demo.document

import com.example.demo.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import tools.jackson.databind.ObjectMapper
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest

import com.example.demo.user.User
import org.springframework.security.core.Authentication

/**
 * Service managing archiving, searching, and analyzing of accounting documents.
 * 
 * It handles uploading files to AWS S3, triggering OCR analysis via Lambda,
 * and indexing the extracted metadata into the database.
 */
@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val userRepository: UserRepository,
    private val pythonAnalyzerClient: PythonAnalyzerClient,
    private val objectMapper: ObjectMapper,
    private val s3Client: S3Client,
    @Value("\${aws.bucket-name}") private val bucketName: String
) {

    /**
     * Uploads an accounting PDF to AWS S3, calls the Python Lambda for OCR/analysis,
     * and persists the resulting metadata in the database.
     * 
     * @param file The uploaded MultipartFile.
     * @param userEmail The email of the user who uploaded the document.
     * @return The saved Document containing the S3 URI and the extracted JSON metadata.
     */
    fun uploadAndProcess(file: MultipartFile, userEmail: String): Document {
        // Find user
        val user = userRepository.findByEmail(userEmail)
            ?: throw IllegalArgumentException("User not found with email: $userEmail")

        val originalFilename = file.originalFilename ?: "document.pdf"
        val uniqueFilename = "${UUID.randomUUID()}_$originalFilename"

        // Upload file to AWS S3
        try {
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueFilename)
                .contentType(file.contentType)
                .build()
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.bytes))
        } catch (e: Exception) {
            e.printStackTrace()
            throw org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to upload document to S3 storage",
                e
            )
        }

        // Call python analyzer (AWS Lambda) to process document
        val extractedJson = try {
            pythonAnalyzerClient.analyze(uniqueFilename)
        } catch (e: Exception) {
            e.printStackTrace()
            // Clean up S3 file if analysis fails to avoid orphan files
            try {
                s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(uniqueFilename)
                        .build()
                )
            } catch (cleanupEx: Exception) {
                cleanupEx.printStackTrace()
            }
            throw org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "Failed to analyze the document. Please verify the document format.",
                e
            )
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

        // Save metadata to DB (storing unique S3 key as filePath)
        val document = Document(
            fileName = originalFilename,
            filePath = uniqueFilename,
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

        // Delete from S3
        try {
            val deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(document.filePath)
                .build()
            s3Client.deleteObject(deleteRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }

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

    fun getDocumentInputStream(id: Long, userEmail: String): InputStream {
        val doc = getDocumentById(id, userEmail)
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(doc.filePath)
            .build()
        return s3Client.getObject(getObjectRequest)
    }

    fun getDistinctTypesByUser(userEmail: String): List<String> {
        val documents = getUserDocuments(userEmail)
        return documents.map { it.type }.distinct()
    }
}
