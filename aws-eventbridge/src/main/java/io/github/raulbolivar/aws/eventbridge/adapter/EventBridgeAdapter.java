package io.github.raulbolivar.aws.eventbridge.adapter;

import io.github.raulbolivar.aws.eventbridge.config.EventBridgeProperties;
import io.github.raulbolivar.aws.eventbridge.exception.EventBridgeException;
import io.github.raulbolivar.aws.eventbridge.model.EventBridgeEvent;
import io.github.raulbolivar.aws.eventbridge.model.EventBridgePublishResult;
import io.github.raulbolivar.aws.eventbridge.port.EventBridgePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClientBuilder;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Default adapter backed by AWS SDK v2 {@link EventBridgeAsyncClient}.
 * Handles batch splitting when more than 10 events are provided (AWS limit).
 *
 * <pre>{@code
 * EventBridgePort bus = EventBridgeAdapter.create(
 *     EventBridgeProperties.builder()
 *         .region("us-east-1")
 *         .defaultEventBus("my-app-bus")
 *         .defaultSource("com.ficohsa.customers")
 *         .build()
 * );
 *
 * bus.publish("com.ficohsa.customers", "CustomerCreated", "{\"id\":\"abc\"}");
 * }</pre>
 */
public class EventBridgeAdapter implements EventBridgePort {

    private static final Logger log          = LoggerFactory.getLogger(EventBridgeAdapter.class);
    private static final int    MAX_BATCH    = 10; // AWS EventBridge hard limit

    private final EventBridgeAsyncClient client;
    private final String                 defaultEventBus;
    private final String                 defaultSource;

    // ─── Factory ─────────────────────────────────────────────────────────────

    public static EventBridgeAdapter create(EventBridgeProperties props) {
        EventBridgeAsyncClientBuilder builder = EventBridgeAsyncClient.builder()
                .region(Region.of(props.getRegion()));

        if (props.getAccessKeyId() != null && props.getSecretAccessKey() != null) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(props.getAccessKeyId(), props.getSecretAccessKey())
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (props.getEndpointOverride() != null) {
            builder.endpointOverride(URI.create(props.getEndpointOverride()));
        }

        return new EventBridgeAdapter(builder.build(), props.getDefaultEventBus(), props.getDefaultSource());
    }

    EventBridgeAdapter(EventBridgeAsyncClient client, String defaultEventBus, String defaultSource) {
        this.client          = client;
        this.defaultEventBus = defaultEventBus;
        this.defaultSource   = defaultSource;
    }

    // ─── Port implementation ──────────────────────────────────────────────────

    @Override
    public EventBridgePublishResult publish(EventBridgeEvent event) {
        return publishAsync(event).join();
    }

    @Override
    public CompletableFuture<EventBridgePublishResult> publishAsync(EventBridgeEvent event) {
        return publishBatchAsync(List.of(event));
    }

    @Override
    public EventBridgePublishResult publishBatch(List<EventBridgeEvent> events) {
        return publishBatchAsync(events).join();
    }

    @Override
    public CompletableFuture<EventBridgePublishResult> publishBatchAsync(List<EventBridgeEvent> events) {
        if (events == null || events.isEmpty()) {
            return CompletableFuture.completedFuture(
                    EventBridgePublishResult.builder().build()
            );
        }

        // Split into chunks of MAX_BATCH to respect AWS limit
        List<List<EventBridgeEvent>> chunks = partition(events, MAX_BATCH);
        List<CompletableFuture<EventBridgePublishResult>> futures = chunks.stream()
                .map(this::publishChunkAsync)
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> mergeResults(futures));
    }

    @Override
    public void publish(String source, String detailType, String detail) {
        EventBridgeEvent event = EventBridgeEvent.builder()
                .source(source)
                .detailType(detailType)
                .detail(detail)
                .build();
        EventBridgePublishResult result = publish(event);
        if (result.hasFailures()) {
            log.warn("EventBridge publish had {} failed entries", result.getFailedEntryCount());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private CompletableFuture<EventBridgePublishResult> publishChunkAsync(List<EventBridgeEvent> chunk) {
        List<PutEventsRequestEntry> entries = chunk.stream()
                .map(this::toSdkEntry)
                .collect(Collectors.toList());

        PutEventsRequest request = PutEventsRequest.builder()
                .entries(entries)
                .build();

        log.debug("Publishing {} events to EventBridge", entries.size());

        return client.putEvents(request)
                .thenApply(this::toPublishResult)
                .exceptionally(ex -> {
                    throw new EventBridgeException(
                            "EventBridge putEvents failed: " + ex.getMessage(),
                            defaultEventBus, ex);
                });
    }

    private PutEventsRequestEntry toSdkEntry(EventBridgeEvent event) {
        String eventBus = event.getEventBusName() != null ? event.getEventBusName() : defaultEventBus;
        String source   = event.getSource() != null ? event.getSource() : defaultSource;

        PutEventsRequestEntry.Builder entry = PutEventsRequestEntry.builder()
                .eventBusName(eventBus)
                .source(source)
                .detailType(event.getDetailType())
                .detail(event.getDetail());

        if (event.getTime() != null) {
            entry.time(Instant.parse(event.getTime()));
        }

        return entry.build();
    }

    private EventBridgePublishResult toPublishResult(PutEventsResponse response) {
        List<String> successIds = new ArrayList<>();
        List<String> failedIds  = new ArrayList<>();

        for (PutEventsResultEntry entry : response.entries()) {
            if (entry.eventId() != null) {
                successIds.add(entry.eventId());
            } else {
                // Failed entries have errorCode/errorMessage instead of eventId
                String failedId = "error:" + entry.errorCode() + ":" + UUID.randomUUID();
                failedIds.add(failedId);
                log.error("EventBridge entry failed — code: {}, message: {}",
                        entry.errorCode(), entry.errorMessage());
            }
        }

        return EventBridgePublishResult.builder()
                .failedEntryCount(response.failedEntryCount())
                .successEventIds(successIds)
                .failedEventIds(failedIds)
                .build();
    }

    private EventBridgePublishResult mergeResults(List<CompletableFuture<EventBridgePublishResult>> futures) {
        List<String> allSuccess = new ArrayList<>();
        List<String> allFailed  = new ArrayList<>();
        int          totalFailed = 0;

        for (CompletableFuture<EventBridgePublishResult> f : futures) {
            EventBridgePublishResult r = f.join();
            allSuccess.addAll(r.getSuccessEventIds());
            allFailed.addAll(r.getFailedEventIds());
            totalFailed += r.getFailedEntryCount();
        }

        return EventBridgePublishResult.builder()
                .failedEntryCount(totalFailed)
                .successEventIds(allSuccess)
                .failedEventIds(allFailed)
                .build();
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
