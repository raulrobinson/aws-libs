package io.github.raulbolivar.aws.parameter.adapter;

import io.github.raulbolivar.aws.parameter.config.ParameterStoreProperties;
import io.github.raulbolivar.aws.parameter.model.ParameterResult;
import io.github.raulbolivar.aws.parameter.port.ParameterStorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmAsyncClient;
import software.amazon.awssdk.services.ssm.SsmAsyncClientBuilder;
import software.amazon.awssdk.services.ssm.model.*;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default adapter backed by AWS SDK v2 {@link SsmAsyncClient}.
 * Includes an in-memory TTL cache (default 12h — intentional, no hot-reload).
 *
 * <pre>{@code
 * ParameterStorePort client = ParameterStoreAdapter.create(
 *     ParameterStoreProperties.builder()
 *         .region("us-east-1")
 *         .pathPrefix("/myapp/prod/")
 *         .build()
 * );
 *
 * String dbUrl = client.getParameterValue("/myapp/prod/db-url").orElseThrow();
 * }</pre>
 */
public class ParameterStoreAdapter implements ParameterStorePort {

    private static final Logger log = LoggerFactory.getLogger(ParameterStoreAdapter.class);

    private final SsmAsyncClient client;
    private final boolean        withDecryption;
    private final long           cacheTtlMillis;

    private final ConcurrentHashMap<String, CachedEntry> cache = new ConcurrentHashMap<>();

    // ─── Factory ─────────────────────────────────────────────────────────────

    public static ParameterStoreAdapter create(ParameterStoreProperties props) {
        SsmAsyncClientBuilder builder = SsmAsyncClient.builder()
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

        return new ParameterStoreAdapter(
                builder.build(),
                props.isDecryptSecureStrings(),
                props.getCacheTtlSeconds() * 1000L
        );
    }

    ParameterStoreAdapter(SsmAsyncClient client, boolean withDecryption, long cacheTtlMillis) {
        this.client         = client;
        this.withDecryption = withDecryption;
        this.cacheTtlMillis = cacheTtlMillis;
    }

    // ─── Port implementation ──────────────────────────────────────────────────

    @Override
    public ParameterResult getParameter(String name) {
        return getParameterAsync(name).join();
    }

    @Override
    public CompletableFuture<ParameterResult> getParameterAsync(String name) {
        CachedEntry cached = cache.get(name);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for parameter: {}", name);
            return CompletableFuture.completedFuture(cached.result);
        }

        log.debug("Fetching parameter from SSM: {}", name);
        return client.getParameter(GetParameterRequest.builder()
                        .name(name)
                        .withDecryption(withDecryption)
                        .build())
                .thenApply(r -> toParameterResult(r.parameter()))
                .thenApply(result -> {
                    cache.put(name, new CachedEntry(result, cacheTtlMillis));
                    return result;
                });
    }

    @Override
    public List<ParameterResult> getParametersByPath(String path) {
        List<ParameterResult> results = new ArrayList<>();
        String nextToken = null;

        do {
            GetParametersByPathRequest.Builder reqBuilder = GetParametersByPathRequest.builder()
                    .path(path)
                    .recursive(true)
                    .withDecryption(withDecryption);

            if (nextToken != null) reqBuilder.nextToken(nextToken);

            GetParametersByPathResponse response = client.getParametersByPath(reqBuilder.build()).join();
            response.parameters().stream()
                    .map(this::toParameterResult)
                    .forEach(r -> {
                        results.add(r);
                        cache.put(r.getName(), new CachedEntry(r, cacheTtlMillis));
                    });

            nextToken = response.nextToken();
        } while (nextToken != null);

        return results;
    }

    @Override
    public Map<String, String> getParametersByPathAsMap(String path) {
        return getParametersByPath(path).stream()
                .collect(Collectors.toMap(ParameterResult::getName, ParameterResult::getValue));
    }

    @Override
    public Optional<String> getParameterValue(String name) {
        return Optional.ofNullable(getParameter(name).getValue());
    }

    @Override
    public void putParameter(String name, String value, boolean overwrite) {
        client.putParameter(PutParameterRequest.builder()
                .name(name)
                .value(value)
                .type(ParameterType.STRING)
                .overwrite(overwrite)
                .build()).join();
        evictCache(name);
        log.info("Parameter stored: {}", name);
    }

    @Override
    public void evictCache(String name) {
        cache.remove(name);
    }

    @Override
    public void evictAllCache() {
        cache.clear();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ParameterResult toParameterResult(Parameter p) {
        ParameterResult.Type type = switch (p.type()) {
            case SECURE_STRING -> ParameterResult.Type.SECURE_STRING;
            case STRING_LIST   -> ParameterResult.Type.STRING_LIST;
            default            -> ParameterResult.Type.STRING;
        };

        return ParameterResult.builder()
                .name(p.name())
                .value(p.value())
                .type(type)
                .version(String.valueOf(p.version()))
                .resolvedAt(Instant.now())
                .build();
    }

    private static final class CachedEntry {
        final ParameterResult result;
        final long            expiresAt;

        CachedEntry(ParameterResult result, long ttlMillis) {
            this.result    = result;
            this.expiresAt = System.currentTimeMillis() + ttlMillis;
        }

        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }
}
