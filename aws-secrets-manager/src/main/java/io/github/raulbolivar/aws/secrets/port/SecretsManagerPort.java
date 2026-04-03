package io.github.raulbolivar.aws.secrets.port;

import io.github.raulbolivar.aws.secrets.model.SecretResult;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Port (hexagonal architecture) for interacting with AWS Secrets Manager.
 * Implementations may be async, cached, or backed by LocalStack.
 */
public interface SecretsManagerPort {

    /**
     * Retrieves a secret synchronously.
     *
     * @param secretName ARN or friendly name of the secret
     * @return the resolved {@link SecretResult}
     */
    SecretResult getSecret(String secretName);

    /**
     * Retrieves a secret asynchronously.
     *
     * @param secretName ARN or friendly name of the secret
     * @return future that resolves to the {@link SecretResult}
     */
    CompletableFuture<SecretResult> getSecretAsync(String secretName);

    /**
     * Parses a JSON-string secret into a key/value map.
     * Useful for secrets that store multiple values as JSON.
     *
     * @param secretName ARN or friendly name of the secret
     * @return map of key → value parsed from the JSON secret string
     */
    Map<String, String> getSecretAsMap(String secretName);

    /**
     * Returns a specific key from a JSON-string secret.
     *
     * @param secretName ARN or friendly name of the secret
     * @param key        key to extract from the JSON object
     * @return optional value for the given key
     */
    Optional<String> getSecretValue(String secretName, String key);

    /**
     * Stores or updates a secret string value.
     *
     * @param secretName  ARN or friendly name of the secret
     * @param secretValue the new secret string value
     */
    void putSecret(String secretName, String secretValue);

    /**
     * Evicts a specific secret from the local cache (if caching is enabled).
     *
     * @param secretName ARN or friendly name of the secret
     */
    void evictCache(String secretName);

    /**
     * Evicts all cached secrets.
     */
    void evictAllCache();
}
