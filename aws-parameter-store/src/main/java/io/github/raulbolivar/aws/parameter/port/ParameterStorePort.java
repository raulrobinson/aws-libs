package io.github.raulbolivar.aws.parameter.port;

import io.github.raulbolivar.aws.parameter.model.ParameterResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Port for AWS SSM Parameter Store operations.
 */
public interface ParameterStorePort {

    /** Retrieves a single parameter by name. */
    ParameterResult getParameter(String name);

    /** Retrieves a single parameter asynchronously. */
    CompletableFuture<ParameterResult> getParameterAsync(String name);

    /** Retrieves all parameters under a path prefix. */
    List<ParameterResult> getParametersByPath(String path);

    /** Retrieves all parameters under a path as a name → value map. */
    Map<String, String> getParametersByPathAsMap(String path);

    /** Returns the value of a parameter directly as a string. */
    Optional<String> getParameterValue(String name);

    /** Puts or updates a String parameter. */
    void putParameter(String name, String value, boolean overwrite);

    /** Evicts a specific parameter from cache. */
    void evictCache(String name);

    /** Evicts all cached parameters. */
    void evictAllCache();
}
