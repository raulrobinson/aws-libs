package io.github.raulbolivar.aws.eventbridge.exception;

/**
 * Unchecked exception thrown when an EventBridge publish operation fails.
 */
public class EventBridgeException extends RuntimeException {

    private final String eventBusName;
    private final int    failedEntries;

    public EventBridgeException(String message, String eventBusName, int failedEntries, Throwable cause) {
        super(message, cause);
        this.eventBusName  = eventBusName;
        this.failedEntries = failedEntries;
    }

    public EventBridgeException(String message, String eventBusName, Throwable cause) {
        this(message, eventBusName, -1, cause);
    }

    public String getEventBusName()  { return eventBusName; }
    public int    getFailedEntries() { return failedEntries; }
}
