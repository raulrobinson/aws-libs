package io.github.raulbolivar.aws.eventbridge.config;

/**
 * Configuration for the AWS EventBridge client.
 *
 * <pre>{@code
 * EventBridgeProperties props = EventBridgeProperties.builder()
 *     .region("us-east-1")
 *     .defaultEventBus("my-app-event-bus")
 *     .defaultSource("com.ficohsa.myapp")
 *     .build();
 * }</pre>
 */
public class EventBridgeProperties {

    private final String region;
    private final String endpointOverride;
    private final String defaultEventBus;   // default: "default"
    private final String defaultSource;     // e.g. "com.myorg.myapp"
    private final String accessKeyId;
    private final String secretAccessKey;

    private EventBridgeProperties(Builder b) {
        this.region           = b.region;
        this.endpointOverride = b.endpointOverride;
        this.defaultEventBus  = b.defaultEventBus;
        this.defaultSource    = b.defaultSource;
        this.accessKeyId      = b.accessKeyId;
        this.secretAccessKey  = b.secretAccessKey;
    }

    public String getRegion()           { return region; }
    public String getEndpointOverride() { return endpointOverride; }
    public String getDefaultEventBus()  { return defaultEventBus; }
    public String getDefaultSource()    { return defaultSource; }
    public String getAccessKeyId()      { return accessKeyId; }
    public String getSecretAccessKey()  { return secretAccessKey; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String region           = "us-east-1";
        private String endpointOverride = null;
        private String defaultEventBus  = "default";
        private String defaultSource    = "com.myorg.myapp";
        private String accessKeyId      = null;
        private String secretAccessKey  = null;

        public Builder region(String v)           { this.region = v;           return this; }
        public Builder endpointOverride(String v) { this.endpointOverride = v; return this; }
        public Builder defaultEventBus(String v)  { this.defaultEventBus = v;  return this; }
        public Builder defaultSource(String v)    { this.defaultSource = v;    return this; }
        public Builder accessKeyId(String v)      { this.accessKeyId = v;      return this; }
        public Builder secretAccessKey(String v)  { this.secretAccessKey = v;  return this; }

        public EventBridgeProperties build() { return new EventBridgeProperties(this); }
    }
}
