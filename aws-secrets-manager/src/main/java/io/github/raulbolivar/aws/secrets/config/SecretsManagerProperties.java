package io.github.raulbolivar.aws.secrets.config;

/**
 * Configuration properties for AWS Secrets Manager client.
 *
 * <pre>{@code
 * SecretsManagerProperties props = SecretsManagerProperties.builder()
 *     .region("us-east-1")
 *     .cacheTtlSeconds(3600)
 *     .build();
 * }</pre>
 */
public class SecretsManagerProperties {

    private final String  region;
    private final String  endpointOverride;   // useful for LocalStack
    private final long    cacheTtlSeconds;
    private final String  accessKeyId;        // optional: static credentials
    private final String  secretAccessKey;    // optional: static credentials

    private SecretsManagerProperties(Builder b) {
        this.region           = b.region;
        this.endpointOverride = b.endpointOverride;
        this.cacheTtlSeconds  = b.cacheTtlSeconds;
        this.accessKeyId      = b.accessKeyId;
        this.secretAccessKey  = b.secretAccessKey;
    }

    public String  getRegion()           { return region; }
    public String  getEndpointOverride() { return endpointOverride; }
    public long    getCacheTtlSeconds()  { return cacheTtlSeconds; }
    public String  getAccessKeyId()      { return accessKeyId; }
    public String  getSecretAccessKey()  { return secretAccessKey; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String  region           = "us-east-1";
        private String  endpointOverride = null;
        private long    cacheTtlSeconds  = 3600L;
        private String  accessKeyId      = null;
        private String  secretAccessKey  = null;

        public Builder region(String region)                     { this.region = region;                     return this; }
        public Builder endpointOverride(String endpointOverride) { this.endpointOverride = endpointOverride; return this; }
        public Builder cacheTtlSeconds(long cacheTtlSeconds)     { this.cacheTtlSeconds = cacheTtlSeconds;   return this; }
        public Builder accessKeyId(String accessKeyId)           { this.accessKeyId = accessKeyId;           return this; }
        public Builder secretAccessKey(String secretAccessKey)   { this.secretAccessKey = secretAccessKey;   return this; }

        public SecretsManagerProperties build() { return new SecretsManagerProperties(this); }
    }
}
