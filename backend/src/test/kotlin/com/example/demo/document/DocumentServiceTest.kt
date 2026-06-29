package com.example.demo.document

import com.example.demo.user.User
import com.example.demo.user.UserRepository
import com.example.demo.user.Role
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import tools.jackson.databind.ObjectMapper

@ExtendWith(MockitoExtension::class)
class DocumentServiceTest {

    @Mock
    lateinit var documentRepository: DocumentRepository

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var pythonAnalyzerClient: PythonAnalyzerClient

    @Mock
    lateinit var s3Client: S3Client

    @Mock
    lateinit var multipartFile: MultipartFile

    private val objectMapper = ObjectMapper()
    private val bucketName = "test-bucket"

    private lateinit var documentService: DocumentService

    @BeforeEach
    fun setUp() {
        documentService = DocumentService(
            documentRepository,
            userRepository,
            pythonAnalyzerClient,
            objectMapper,
            s3Client,
            bucketName
        )
    }

    @Test
    fun `uploadAndProcess uploads to S3, calls analyzer, and saves metadata`() {
        val email = "user@example.com"
        val user = User(id = 1L, email = email, password = "password", role = Role.USER)
        
        val fileBytes = "dummy pdf content".toByteArray()
        val originalFilename = "invoice.pdf"
        val contentType = "application/pdf"
        val extractedJson = """{"extracted_data": {"document_type": "facture", "total": 100.0}}"""

        `when`(userRepository.findByEmail(email)).thenReturn(user)
        `when`(multipartFile.originalFilename).thenReturn(originalFilename)
        `when`(multipartFile.contentType).thenReturn(contentType)
        `when`(multipartFile.bytes).thenReturn(fileBytes)
        
        `when`(pythonAnalyzerClient.analyze(anyString())).thenReturn(extractedJson)
        
        // Mock save returning the document
        `when`(documentRepository.save(any(Document::class.java))).thenAnswer { invocation ->
            invocation.arguments[0] as Document
        }

        val result = documentService.uploadAndProcess(multipartFile, email)

        assertNotNull(result)
        assertEquals(originalFilename, result.fileName)
        assertEquals("facture", result.type)
        assertEquals(user, result.user)
        assertEquals(extractedJson, result.extractedJson)

        // Verify S3 putObject was called
        verify(s3Client).putObject(any(PutObjectRequest::class.java), any(RequestBody::class.java))
        // Verify python Lambda client was called
        verify(pythonAnalyzerClient).analyze(anyString())
        // Verify repository save was called
        verify(documentRepository).save(any(Document::class.java))
    }
}
