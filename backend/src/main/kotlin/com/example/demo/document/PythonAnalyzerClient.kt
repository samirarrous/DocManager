package com.example.demo.document

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import software.amazon.awssdk.core.SdkBytes
import tools.jackson.databind.ObjectMapper

/**
 * Client service responsible for communicating with the Python document analysis engine.
 * 
 * Instead of making a local REST HTTP call, this client directly invokes
 * an AWS Lambda function (hosted as a Docker ECR container image)
 * by providing it S3 references of the document to analyze.
 */
@Service
class PythonAnalyzerClient(
    @Value("\${aws.bucket-name}") private val bucketName: String,
    @Value("\${aws.lambda.analyzer-name:docmanager-analyzer}") private val lambdaFunctionName: String,
    private val objectMapper: ObjectMapper,
    private val lambdaClient: LambdaClient
) {

    /**
     * Synchronously invokes the AWS Lambda function to analyze a document stored on S3.
     * 
     * @param s3Key The unique key of the file in the S3 bucket.
     * @return The raw JSON result containing OCR analysis and key data extraction.
     * @throws RuntimeException If the Lambda invocation or document processing fails.
     */
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