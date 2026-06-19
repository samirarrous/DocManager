package com.example.demo.document

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

@Service
class PythonAnalyzerClient(
    @Value("\${python.analyzer.url:http://localhost:8000/upload}") private val url: String
) {
    private val restTemplate = RestTemplate()

    fun analyze(fileBytes: ByteArray, originalFilename: String): String {
        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val body = LinkedMultiValueMap<String, Any>()
        val fileResource = object : ByteArrayResource(fileBytes) {
            override fun getFilename(): String {
                return originalFilename
            }
        }
        body.add("file", fileResource)

        val request = HttpEntity(body, headers)

        return restTemplate.postForObject(url, request, String::class.java)
            ?: throw RuntimeException("Analyzer failed")
    }
}