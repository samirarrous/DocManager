package com.example.demo.document

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import software.amazon.awssdk.core.SdkBytes
import tools.jackson.databind.ObjectMapper

@Service
class PythonAnalyzerClient(
    @Value("\${aws.bucket-name}") private val bucketName: String,
    @Value("\${aws.lambda.analyzer-name:docmanager-analyzer}") private val lambdaFunctionName: String,
    private val objectMapper: ObjectMapper,
    private val lambdaClient: LambdaClient
) {

    fun analyze(s3Key: String): String {
        val payloadMap = mapOf(
            "bucket" to bucketName,
            "key" to s3Key
        )
        val payloadJson = objectMapper.writeValueAsString(payloadMap)

        val invokeRequest = InvokeRequest.builder()
            .functionName(lambdaFunctionName)
            .payload(SdkBytes.fromUtf8String(payloadJson))
            .build()

        val response = lambdaClient.invoke(invokeRequest)
        val responsePayload = response.payload().asUtf8String()

        if (response.functionError() != null) {
            throw RuntimeException("Lambda analysis failed: $responsePayload")
        }

        val responseMap = objectMapper.readValue(responsePayload, Map::class.java)
        val body = responseMap["body"]

        return if (body is Map<*, *>) {
            objectMapper.writeValueAsString(body)
        } else {
            body?.toString() ?: throw RuntimeException("Empty response body from Lambda")
        }
    }
}