package io.github.raulbolivar.aws.lambda.adapter;

import io.github.raulbolivar.aws.lambda.config.LambdaClientProperties;
import io.github.raulbolivar.aws.lambda.exception.LambdaClientException;
import io.github.raulbolivar.aws.lambda.model.LambdaInvocationRequest;
import io.github.raulbolivar.aws.lambda.model.LambdaInvocationResult;
import io.github.raulbolivar.aws.lambda.port.LambdaClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.LambdaAsyncClientBuilder;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.InvocationType;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Default adapter backed by AWS SDK v2 {@link LambdaAsyncClient}.
 *
 * <pre>{@code
 * LambdaClientPort lambda = LambdaClientAdapter.create(
 *     LambdaClientProperties.builder()
 *         .region("us-east-1")
 *         .build()
 * );
 *
 * String response = lambda.invokeAndGetPayload("my-function", "{\"op\":\"ping\"}");
 * }</pre>
 */
public class LambdaClientAdapter implements LambdaClientPort {

    private static final Logger log = LoggerFactory.getLogger(LambdaClientAdapter.class);

    private final LambdaAsyncClient client;

    // ─── Factory ─────────────────────────────────────────────────────────────

    public static LambdaClientAdapter create(LambdaClientProperties props) {
        LambdaAsyncClientBuilder builder = LambdaAsyncClient.builder()
                .region(Region.of(props.getRegion()));

        if (props.getAccessKeyId() != null && props.getSecretAccessKey() != null) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(props.getAccessKeyId(), props.getSecretAccessKey())
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (props.getEndpointOverride() != null) {
            builder.endpointOverride(URI.create(props.getEndpointOverride()));
        }

        return new LambdaClientAdapter(builder.build());
    }

    LambdaClientAdapter(LambdaAsyncClient client) {
        this.client = client;
    }

    // ─── Port implementation ──────────────────────────────────────────────────

    @Override
    public LambdaInvocationResult invoke(LambdaInvocationRequest request) {
        try {
            return invokeAsync(request).join();
        } catch (LambdaClientException e) {
            throw e;
        } catch (Exception e) {
            throw new LambdaClientException("Lambda invocation failed: " + request.getFunctionName(),
                    request.getFunctionName(), e);
        }
    }

    @Override
    public CompletableFuture<LambdaInvocationResult> invokeAsync(LambdaInvocationRequest request) {
        InvokeRequest sdkRequest = buildSdkRequest(request);

        log.debug("Invoking Lambda: {} [{}]", request.getFunctionName(), request.getInvocationType());

        return client.invoke(sdkRequest)
                .thenApply(response -> toResult(request.getFunctionName(), response))
                .exceptionally(ex -> {
                    throw new LambdaClientException(
                            "Async Lambda invocation failed: " + request.getFunctionName(),
                            request.getFunctionName(), ex);
                });
    }

    @Override
    public void invokeAsync(String functionName, String payload) {
        InvokeRequest sdkRequest = InvokeRequest.builder()
                .functionName(functionName)
                .invocationType(InvocationType.EVENT)
                .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                .build();

        log.debug("Fire-and-forget Lambda: {}", functionName);
        client.invoke(sdkRequest)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Fire-and-forget Lambda error [{}]: {}", functionName, ex.getMessage());
                });
    }

    @Override
    public String invokeAndGetPayload(String functionName, String jsonPayload) {
        LambdaInvocationRequest req = LambdaInvocationRequest.builder()
                .functionName(functionName)
                .payload(jsonPayload)
                .build();
        LambdaInvocationResult result = invoke(req);
        if (result.isError()) {
            throw new LambdaClientException(
                    "Lambda returned function error [" + result.getFunctionError() + "]: " + result.getPayload(),
                    functionName, result.getStatusCode(), null);
        }
        return result.getPayload();
    }

    @Override
    public CompletableFuture<String> invokeAndGetPayloadAsync(String functionName, String jsonPayload) {
        LambdaInvocationRequest req = LambdaInvocationRequest.builder()
                .functionName(functionName)
                .payload(jsonPayload)
                .build();
        return invokeAsync(req).thenApply(result -> {
            if (result.isError()) {
                throw new LambdaClientException(
                        "Lambda returned function error [" + result.getFunctionError() + "]: " + result.getPayload(),
                        functionName, result.getStatusCode(), null);
            }
            return result.getPayload();
        });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private InvokeRequest buildSdkRequest(LambdaInvocationRequest req) {
        InvocationType sdkType = switch (req.getInvocationType()) {
            case EVENT    -> InvocationType.EVENT;
            case DRY_RUN  -> InvocationType.DRY_RUN;
            default       -> InvocationType.REQUEST_RESPONSE;
        };

        InvokeRequest.Builder builder = InvokeRequest.builder()
                .functionName(req.getFunctionName())
                .invocationType(sdkType)
                .payload(SdkBytes.fromString(req.getPayload(), StandardCharsets.UTF_8));

        if (req.getQualifier() != null) {
            builder.qualifier(req.getQualifier());
        }

        return builder.build();
    }

    private LambdaInvocationResult toResult(String functionName, InvokeResponse response) {
        String payload = response.payload() != null
                ? response.payload().asString(StandardCharsets.UTF_8)
                : null;

        return LambdaInvocationResult.builder()
                .functionName(functionName)
                .statusCode(response.statusCode())
                .payload(payload)
                .functionError(response.functionError())
                .logResult(response.logResult())
                .executedVersion(response.executedVersion())
                .build();
    }
}
