package io.github.raulbolivar.aws.parameter.config;

/**
 * Configuration for the AWS SSM Parameter Store client.
 *
 * <pre>{@code
 * ParameterStoreProperties props = ParameterStoreProperties.builder()
 *     .region("us-east-1")
 *     .pathPrefix("/myapp/prod/")
 *     .cacheTtlSeconds(43200)
 *     .build();
 * }</pre>
 */
public class ParameterStoreProperties {

    private final String  region;
    private final String  endpointOverride;
    private final String  pathPrefix;       // e.g. "/myapp/prod/"
    private final long    cacheTtlSeconds;
    private final boolean decryptSecureStrings;
    private final String  accessKeyId;
    private final String  secretAccessKey;

    private ParameterStoreProperties(Builder b) {
        this.region               = b.region;
        this.endpointOverride     = b.endpointOverride;
        this.pathPrefix           = b.pathPrefix;
        this.cacheTtlSeconds      = b.cacheTtlSeconds;
        this.decryptSecureStrings = b.decryptSecureStrings;
        this.accessKeyId          = b.accessKeyId;
        this.secretAccessKey      = b.secretAccessKey;
    }

    public String  getRegion()               { return region; }
    public String  getEndpointOverride()     { return endpointOverride; }
    public String  getPathPrefix()           { return pathPrefix; }
    public long    getCacheTtlSeconds()      { return cacheTtlSeconds; }
    public boolean isDecryptSecureStrings()  { return decryptSecureStrings; }
    public String  getAccessKeyId()          { return accessKeyId; }
    public String  getSecretAccessKey()      { return secretAccessKey; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String  region               = "us-east-1";
        private String  endpointOverride     = null;
        private String  pathPrefix           = "/";
        private long    cacheTtlSeconds      = 43200L; // 12h intentional — no hot-reload
        private boolean decryptSecureStrings = true;
        private String  accessKeyId          = null;
        private String  secretAccessKey      = null;

        public Builder region(String v)               { this.region = v;               return this; }
        public Builder endpointOverride(String v)     { this.endpointOverride = v;     return this; }
        public Builder pathPrefix(String v)           { this.pathPrefix = v;           return this; }
        public Builder cacheTtlSeconds(long v)        { this.cacheTtlSeconds = v;      return this; }
        public Builder decryptSecureStrings(boolean v){ this.decryptSecureStrings = v; return this; }
        public Builder accessKeyId(String v)          { this.accessKeyId = v;          return this; }
        public Builder secretAccessKey(String v)      { this.secretAccessKey = v;      return this; }

        public ParameterStoreProperties build() { return new ParameterStoreProperties(this); }
    }
}
