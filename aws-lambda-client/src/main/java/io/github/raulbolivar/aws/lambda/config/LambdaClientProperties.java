package io.github.raulbolivar.aws.lambda.config;

/**
 * Configuration for the AWS Lambda invocation client.
 */
public class LambdaClientProperties {

    private final String region;
    private final String endpointOverride;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final int    connectionTimeoutSeconds;
    private final int    readTimeoutSeconds;

    private LambdaClientProperties(Builder b) {
        this.region                   = b.region;
        this.endpointOverride         = b.endpointOverride;
        this.accessKeyId              = b.accessKeyId;
        this.secretAccessKey          = b.secretAccessKey;
        this.connectionTimeoutSeconds = b.connectionTimeoutSeconds;
        this.readTimeoutSeconds       = b.readTimeoutSeconds;
    }

    public String getRegion()                   { return region; }
    public String getEndpointOverride()         { return endpointOverride; }
    public String getAccessKeyId()              { return accessKeyId; }
    public String getSecretAccessKey()          { return secretAccessKey; }
    public int    getConnectionTimeoutSeconds() { return connectionTimeoutSeconds; }
    public int    getReadTimeoutSeconds()       { return readTimeoutSeconds; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String region                   = "us-east-1";
        private String endpointOverride         = null;
        private String accessKeyId              = null;
        private String secretAccessKey          = null;
        private int    connectionTimeoutSeconds = 10;
        private int    readTimeoutSeconds       = 60;

        public Builder region(String v)                   { this.region = v;                   return this; }
        public Builder endpointOverride(String v)         { this.endpointOverride = v;         return this; }
        public Builder accessKeyId(String v)              { this.accessKeyId = v;              return this; }
        public Builder secretAccessKey(String v)          { this.secretAccessKey = v;          return this; }
        public Builder connectionTimeoutSeconds(int v)    { this.connectionTimeoutSeconds = v; return this; }
        public Builder readTimeoutSeconds(int v)          { this.readTimeoutSeconds = v;       return this; }

        public LambdaClientProperties build() { return new LambdaClientProperties(this); }
    }
}
