package com.example.demo.document

import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class DocumentSearchService(
    private val documentService: DocumentService,
    private val objectMapper: ObjectMapper
) {

    fun getAvailableTypes(userEmail: String): List<String> {
        return documentService.getDistinctTypesByUser(userEmail)
    }

    fun search(
        year: String?,
        type: String?,
        query: String?,
        userEmail: String
    ): List<Document> {
        var documents = documentService.getUserDocuments(userEmail)

        // 1. Filter by type
        if (!type.isNullOrEmpty()) {
            documents = documents.filter { doc ->
                doc.type.equals(type, ignoreCase = true)
            }
        }

        // 2. Filter by year
        if (!year.isNullOrEmpty()) {
            documents = documents.filter { doc ->
                try {
                    val json = doc.extractedJson
                    if (json.isNullOrEmpty()) {
                        false
                    } else {
                        val map = objectMapper.readValue(json, Map::class.java) as Map<*, *>
                        map.any { (k, v) ->
                            val key = k.toString()
                            val value = v?.toString() ?: ""
                            (key.contains("date", true) || key.contains("year", true))
                                    && value.contains(year)
                        }
                    }
                } catch (e: Exception) {
                    false
                }
            }
        }

        // 3. Filter by general field query
        if (!query.isNullOrEmpty()) {
            documents = documents.filter { doc ->
                try {
                    val json = doc.extractedJson
                    if (json.isNullOrEmpty()) {
                        false
                    } else {
                        val map = objectMapper.readValue(json, Map::class.java) as Map<*, *>
                        map.any { (_, v) ->
                            v?.toString()?.contains(query, ignoreCase = true) == true
                        }
                    }
                } catch (e: Exception) {
                    false
                }
            }
        }

        return documents
    }
}
