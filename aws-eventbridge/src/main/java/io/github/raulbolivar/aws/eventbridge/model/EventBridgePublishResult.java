package io.github.raulbolivar.aws.eventbridge.model;

import java.util.List;

/**
 * Result of publishing one or more events to AWS EventBridge.
 */
public class EventBridgePublishResult {

    private final int          failedEntryCount;
    private final List<String> failedEventIds;
    private final List<String> successEventIds;

    private EventBridgePublishResult(Builder b) {
        this.failedEntryCount = b.failedEntryCount;
        this.failedEventIds   = b.failedEventIds != null ? List.copyOf(b.failedEventIds) : List.of();
        this.successEventIds  = b.successEventIds != null ? List.copyOf(b.successEventIds) : List.of();
    }

    public int          getFailedEntryCount() { return failedEntryCount; }
    public List<String> getFailedEventIds()   { return failedEventIds; }
    public List<String> getSuccessEventIds()  { return successEventIds; }

    public boolean isFullSuccess() { return failedEntryCount == 0; }
    public boolean hasFailures()   { return failedEntryCount > 0; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int          failedEntryCount = 0;
        private List<String> failedEventIds;
        private List<String> successEventIds;

        public Builder failedEntryCount(int v)          { this.failedEntryCount = v; return this; }
        public Builder failedEventIds(List<String> v)   { this.failedEventIds = v;  return this; }
        public Builder successEventIds(List<String> v)  { this.successEventIds = v; return this; }

        public EventBridgePublishResult build() { return new EventBridgePublishResult(this); }
    }
}
