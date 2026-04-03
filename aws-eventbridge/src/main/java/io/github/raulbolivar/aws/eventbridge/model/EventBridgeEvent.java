package io.github.raulbolivar.aws.eventbridge.model;

/**
 * Represents an event to be published to AWS EventBridge.
 *
 * <pre>{@code
 * EventBridgeEvent event = EventBridgeEvent.builder()
 *     .source("com.ficohsa.customers")
 *     .detailType("CustomerCreated")
 *     .detail("{\"customerId\":\"abc-123\",\"plan\":\"premium\"}")
 *     .eventBusName("my-app-bus")
 *     .build();
 * }</pre>
 */
public class EventBridgeEvent {

    private final String source;
    private final String detailType;
    private final String detail;         // JSON string
    private final String eventBusName;
    private final String resources;      // optional ARN list as comma-separated string
    private final String time;           // optional ISO-8601 timestamp

    private EventBridgeEvent(Builder b) {
        this.source       = b.source;
        this.detailType   = b.detailType;
        this.detail       = b.detail;
        this.eventBusName = b.eventBusName;
        this.resources    = b.resources;
        this.time         = b.time;
    }

    public String getSource()       { return source; }
    public String getDetailType()   { return detailType; }
    public String getDetail()       { return detail; }
    public String getEventBusName() { return eventBusName; }
    public String getResources()    { return resources; }
    public String getTime()         { return time; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String source;
        private String detailType;
        private String detail       = "{}";
        private String eventBusName = null;   // null = use default from properties
        private String resources    = null;
        private String time         = null;

        public Builder source(String v)       { this.source = v;       return this; }
        public Builder detailType(String v)   { this.detailType = v;   return this; }
        public Builder detail(String v)       { this.detail = v;       return this; }
        public Builder eventBusName(String v) { this.eventBusName = v; return this; }
        public Builder resources(String v)    { this.resources = v;    return this; }
        public Builder time(String v)         { this.time = v;         return this; }

        public EventBridgeEvent build() {
            if (source == null || source.isBlank())
                throw new IllegalArgumentException("source is required");
            if (detailType == null || detailType.isBlank())
                throw new IllegalArgumentException("detailType is required");
            return new EventBridgeEvent(this);
        }
    }
}
