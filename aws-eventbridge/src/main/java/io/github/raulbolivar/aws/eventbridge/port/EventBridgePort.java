package io.github.raulbolivar.aws.eventbridge.port;

import io.github.raulbolivar.aws.eventbridge.model.EventBridgeEvent;
import io.github.raulbolivar.aws.eventbridge.model.EventBridgePublishResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Port (hexagonal architecture) for publishing events to AWS EventBridge.
 */
public interface EventBridgePort {

    /**
     * Publishes a single event synchronously.
     *
     * @param event the event to publish
     * @return publish result with success/failure counts
     */
    EventBridgePublishResult publish(EventBridgeEvent event);

    /**
     * Publishes a single event asynchronously.
     *
     * @param event the event to publish
     * @return future resolving to the publish result
     */
    CompletableFuture<EventBridgePublishResult> publishAsync(EventBridgeEvent event);

    /**
     * Publishes multiple events in a batch (max 10 per AWS limit).
     *
     * @param events list of events to publish
     * @return publish result with success/failure counts
     */
    EventBridgePublishResult publishBatch(List<EventBridgeEvent> events);

    /**
     * Publishes multiple events asynchronously in a batch.
     *
     * @param events list of events to publish
     * @return future resolving to the publish result
     */
    CompletableFuture<EventBridgePublishResult> publishBatchAsync(List<EventBridgeEvent> events);

    /**
     * Convenience method: publish a raw JSON detail with source and detailType.
     *
     * @param source     event source identifier (e.g. "com.myorg.myservice")
     * @param detailType event type label (e.g. "OrderCreated")
     * @param detail     JSON string payload
     */
    void publish(String source, String detailType, String detail);
}
