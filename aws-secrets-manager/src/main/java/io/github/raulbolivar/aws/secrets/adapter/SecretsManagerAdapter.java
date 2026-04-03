package io.github.raulbolivar.aws.secrets.adapter;

import io.github.raulbolivar.aws.secrets.config.SecretsManagerProperties;
import io.github.raulbolivar.aws.secrets.exception.SecretsManagerException;
import io.github.raulbolivar.aws.secrets.model.SecretResult;
import io.github.raulbolivar.aws.secrets.port.SecretsManagerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default adapter backed by AWS SDK v2 {@link SecretsManagerAsyncClient}.
 * Includes an in-memory TTL cache to reduce API calls.
 *
 * <pre>{@code
 * SecretsManagerPort client = SecretsManagerAdapter.create(
 *     SecretsManagerProperties.builder()
 *         .region("us-east-1")
 *         .cacheTtlSeconds(3600)
 *         .build()
 * );
 *
 * SecretResult result = client.getSecret("my/secret/name");
 * }</pre>
 */
public class SecretsManagerAdapter implements SecretsManagerPort {

    private static final Logger log = LoggerFactory.getLogger(SecretsManagerAdapter.class);

    private final SecretsManagerAsyncClient client;
    private final long cacheTtlMillis;

    // Simple in-memory TTL cache: secretName → (result, expiresAt)
    private final ConcurrentHashMap<String, CachedEntry> cache = new ConcurrentHashMap<>();

    // ─── Factory ─────────────────────────────────────────────────────────────

    public static SecretsManagerAdapter create(SecretsManagerProperties props) {
        SecretsManagerAsyncClientBuilder builder = SecretsManagerAsyncClient.builder()
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

        return new SecretsManagerAdapter(builder.build(), props.getCacheTtlSeconds() * 1000L);
    }

    SecretsManagerAdapter(SecretsManagerAsyncClient client, long cacheTtlMillis) {
        this.client         = client;
        this.cacheTtlMillis = cacheTtlMillis;
    }

    // ─── Port implementation ──────────────────────────────────────────────────

    @Override
    public SecretResult getSecret(String secretName) {
        try {
            return getSecretAsync(secretName).join();
        } catch (Exception e) {
            throw new SecretsManagerException("Failed to get secret: " + secretName, secretName, e);
        }
    }

    @Override
    public CompletableFuture<SecretResult> getSecretAsync(String secretName) {
        CachedEntry cached = cache.get(secretName);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for secret: {}", secretName);
            return CompletableFuture.completedFuture(cached.result);
        }

        log.debug("Cache miss — fetching secret from AWS: {}", secretName);
        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        return client.getSecretValue(request)
                .thenApply(this::toSecretResult)
                .thenApply(result -> {
                    cache.put(secretName, new CachedEntry(result, cacheTtlMillis));
                    return result;
                })
                .exceptionally(ex -> {
                    throw new SecretsManagerException(
                            "Failed to retrieve secret asynchronously: " + secretName,
                            secretName, ex);
                });
    }

    @Override
    public Map<String, String> getSecretAsMap(String secretName) {
        SecretResult result = getSecret(secretName);
        if (!result.isString()) {
            throw new SecretsManagerException(
                    "Secret is binary, cannot parse as JSON map: " + secretName, secretName, null);
        }
        return parseJsonToMap(result.getSecretString());
    }

    @Override
    public Optional<String> getSecretValue(String secretName, String key) {
        Map<String, String> map = getSecretAsMap(secretName);
        return Optional.ofNullable(map.get(key));
    }

    @Override
    public void putSecret(String secretName, String secretValue) {
        try {
            PutSecretValueRequest request = PutSecretValueRequest.builder()
                    .secretId(secretName)
                    .secretString(secretValue)
                    .build();
            client.putSecretValue(request).join();
            evictCache(secretName);
            log.info("Secret updated: {}", secretName);
        } catch (Exception e) {
            throw new SecretsManagerException("Failed to put secret: " + secretName, secretName, e);
        }
    }

    @Override
    public void evictCache(String secretName) {
        cache.remove(secretName);
        log.debug("Cache evicted for secret: {}", secretName);
    }

    @Override
    public void evictAllCache() {
        cache.clear();
        log.debug("Full secrets cache evicted");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private SecretResult toSecretResult(GetSecretValueResponse response) {
        SecretResult.Builder builder = SecretResult.builder()
                .secretName(response.name())
                .versionId(response.versionId())
                .resolvedAt(Instant.now());

        if (response.secretString() != null) {
            builder.secretString(response.secretString());
        } else if (response.secretBinary() != null) {
            builder.secretBinary(response.secretBinary().asByteArray());
        }

        return builder.build();
    }

    private Map<String, String> parseJsonToMap(String json) {
        // Lightweight JSON parser — avoids requiring Jackson as a dependency.
        // For complex nested secrets, callers should apply their own JSON library.
        Map<String, String> map = new HashMap<>();
        String stripped = json.trim().replaceAll("^\\{|\\}$", "");
        for (String pair : stripped.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key   = kv[0].trim().replaceAll("^\"|\"$", "");
                String value = kv[1].trim().replaceAll("^\"|\"$", "");
                map.put(key, value);
            }
        }
        return map;
    }

    // ─── Cache entry ──────────────────────────────────────────────────────────

    private static final class CachedEntry {
        final SecretResult result;
        final long         expiresAt;

        CachedEntry(SecretResult result, long ttlMillis) {
            this.result    = result;
            this.expiresAt = System.currentTimeMillis() + ttlMillis;
        }

        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }
}
