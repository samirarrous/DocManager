package com.example.demo.document

import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import com.example.demo.user.UserRepository
import com.example.demo.user.Role

@Service
class DocumentSearchService(
    private val documentService: DocumentService,
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepository
) {

    fun getAvailableTypes(userEmail: String): List<String> {
        return documentService.getDistinctTypesByUser(userEmail)
    }

    fun search(
        year: String?,
        type: String?,
        query: String?,
        userEmail: String,
        targetUser: String? = null
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
                        
                        fun containsYear(valueMap: Map<*, *>, searchYear: String): Boolean {
                            return valueMap.any { (k, v) ->
                                val key = k.toString()
                                when (v) {
                                    is Map<*, *> -> containsYear(v, searchYear)
                                    is List<*> -> v.any { item ->
                                        item is Map<*, *> && containsYear(item, searchYear)
                                    }
                                    else -> {
                                        val valueStr = v?.toString() ?: ""
                                        (key.contains("date", true) || key.contains("year", true))
                                                && valueStr.contains(searchYear)
                                    }
                                }
                            }
                        }
                        
                        containsYear(map, year)
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
                        
                        fun containsQuery(valueMap: Map<*, *>, searchQuery: String): Boolean {
                            return valueMap.any { (_, v) ->
                                when (v) {
                                    is Map<*, *> -> containsQuery(v, searchQuery)
                                    is List<*> -> v.any { item ->
                                        (item is Map<*, *> && containsQuery(item, searchQuery)) ||
                                        item?.toString()?.contains(searchQuery, ignoreCase = true) == true
                                    }
                                    else -> v?.toString()?.contains(searchQuery, ignoreCase = true) == true
                                }
                            }
                        }
                        
                        containsQuery(map, query)
                    }
                } catch (e: Exception) {
                    false
                }
            }
        }

        // 3.Filter by username (for admin only)
        val user = userRepository.findByEmail(userEmail)
        if (user?.role == Role.ADMIN) {
            if (!targetUser.isNullOrEmpty()) {
                documents = documents.filter { doc ->
                    doc.user.email.equals(targetUser, ignoreCase = true)
                }
            }
        }

        return documents
    }
}
