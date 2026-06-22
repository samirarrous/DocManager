package com.example.demo.document

import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

import org.springframework.security.core.Authentication

@RestController
@RequestMapping("/documents")
class DocumentController(
    private val documentService: DocumentService,
    private val documentSearchService: DocumentSearchService
) {

    @PostMapping("/upload")
    fun upload(
        @RequestParam("file") file: MultipartFile,
        @RequestAttribute("currentUserEmail") currentUserEmail: String
    ): ResponseEntity<Document> {
        val doc = documentService.uploadAndProcess(file, currentUserEmail)
        return ResponseEntity.ok(doc)
    }

    @GetMapping
    fun list(@RequestAttribute("currentUserEmail") currentUserEmail: String): ResponseEntity<List<Document>> {
        val docs = documentService.getUserDocuments(currentUserEmail)
        return ResponseEntity.ok(docs)
    }

    @GetMapping("/{id}/download")
    fun download(
        @PathVariable id: Long,
        @RequestAttribute("currentUserEmail") currentUserEmail: String
    ): ResponseEntity<Resource> {

        val file = documentService.getDocumentFile(id, currentUserEmail)
        val resource = FileSystemResource(file)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${file.name}\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(resource)
    }


    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
        @RequestAttribute("currentUserEmail") currentUserEmail: String
    ): ResponseEntity<Document> {
        val doc = documentService.getDocumentById(id, currentUserEmail)
        return ResponseEntity.ok(doc)
    }


    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<Void> {
        documentService.deleteDocument(id, authentication)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/types")
    fun getTypes(@RequestAttribute("currentUserEmail") currentUserEmail: String): List<String> {
        return documentSearchService.getAvailableTypes(currentUserEmail)
    }

    @GetMapping("/search")
    fun search(
        @RequestParam(required = false) year: String?,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) targetUser: String?,
        @RequestAttribute("currentUserEmail") currentUserEmail: String
    ): ResponseEntity<List<Document>> {
        val docs = documentSearchService.search(year, type, query, currentUserEmail, targetUser)
        return ResponseEntity.ok(docs)
    }
}
