package io.github.raulbolivar.aws.lambda.port;

import io.github.raulbolivar.aws.lambda.model.LambdaInvocationRequest;
import io.github.raulbolivar.aws.lambda.model.LambdaInvocationResult;

import java.util.concurrent.CompletableFuture;

/**
 * Port (hexagonal architecture) for invoking AWS Lambda functions.
 */
public interface LambdaClientPort {

    /**
     * Invokes a Lambda function synchronously (RequestResponse).
     *
     * @param request invocation parameters
     * @return the invocation result including response payload
     */
    LambdaInvocationResult invoke(LambdaInvocationRequest request);

    /**
     * Invokes a Lambda function asynchronously (RequestResponse).
     *
     * @param request invocation parameters
     * @return future that resolves to the invocation result
     */
    CompletableFuture<LambdaInvocationResult> invokeAsync(LambdaInvocationRequest request);

    /**
     * Fire-and-forget invocation (Event type — no response payload).
     *
     * @param functionName ARN or name of the Lambda function
     * @param payload      JSON string payload
     */
    void invokeAsync(String functionName, String payload);

    /**
     * Convenience method: invoke with a raw JSON string and get the response payload.
     *
     * @param functionName ARN or name of the Lambda function
     * @param jsonPayload  JSON string to send as event
     * @return JSON string response from the function
     */
    String invokeAndGetPayload(String functionName, String jsonPayload);

    /**
     * Convenience method: invoke asynchronously and get the response payload.
     *
     * @param functionName ARN or name of the Lambda function
     * @param jsonPayload  JSON string to send as event
     * @return future resolving to JSON string response
     */
    CompletableFuture<String> invokeAndGetPayloadAsync(String functionName, String jsonPayload);
}
