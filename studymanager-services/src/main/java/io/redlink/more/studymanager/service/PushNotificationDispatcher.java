/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.service;

import io.redlink.more.studymanager.properties.PushServiceProperties;
import io.redlink.more.studymanager.push.client.api.NotificationsApi;
import io.redlink.more.studymanager.push.client.model.PushNotificationAccepted;
import io.redlink.more.studymanager.push.client.model.PushNotificationRequest;
import io.redlink.more.studymanager.push.client.model.SkippedRecipient;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Sends push-notifications to the MORE push-service.
 * <p>
 * The push-service is not required for the study-manager to run: a notification that cannot be
 * handed over (service down, timeout, server-error) is parked in a bounded in-memory queue and
 * retried by a single worker with an increasing backoff. Nothing is ever thrown at the caller.
 */
@Service
public class PushNotificationDispatcher {

    public enum Outcome {
        /** The push-service accepted the notification for delivery. */
        DELIVERED,
        /** The push-service could not be reached; the notification is queued for retry. */
        QUEUED,
        /** The push-service refused the notification; retrying cannot help. */
        REJECTED
    }

    private enum Attempt {
        DELIVERED, REJECTED, RETRY
    }

    private static final Logger LOG = LoggerFactory.getLogger(PushNotificationDispatcher.class);

    private final NotificationsApi notificationsApi;
    private final PushServiceProperties.Retry config;
    private final ScheduledExecutorService scheduler;
    private final LinkedBlockingDeque<PendingPush> queue;
    private final AtomicBoolean drainScheduled = new AtomicBoolean(false);

    private volatile Duration currentDelay;

    @Autowired
    public PushNotificationDispatcher(NotificationsApi notificationsApi, PushServiceProperties properties) {
        this(notificationsApi, properties, Executors.newSingleThreadScheduledExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "push-notification-retry");
            thread.setDaemon(true);
            return thread;
        }));
    }

    PushNotificationDispatcher(NotificationsApi notificationsApi, PushServiceProperties properties,
                               ScheduledExecutorService scheduler) {
        this.notificationsApi = notificationsApi;
        this.config = properties.retry();
        this.scheduler = scheduler;
        this.queue = new LinkedBlockingDeque<>(config.queueCapacity());
        this.currentDelay = config.initialDelay();
    }

    /**
     * Hand a notification to the push-service, retrying in the background if it is unreachable.
     */
    public Outcome send(PushNotificationRequest request) {
        return switch (attempt(request)) {
            case DELIVERED -> Outcome.DELIVERED;
            case REJECTED -> Outcome.REJECTED;
            case RETRY -> {
                enqueue(new PendingPush(request, Instant.now(), 1));
                yield Outcome.QUEUED;
            }
        };
    }

    private Attempt attempt(PushNotificationRequest request) {
        try {
            final PushNotificationAccepted response = notificationsApi.sendNotification(request);
            final int accepted = response == null || response.getAccepted() == null ? 0 : response.getAccepted();
            if (accepted > 0) {
                LOG.debug("Push-service accepted {} message(s) (batch:{})", accepted,
                        response.getBatchId());
                return Attempt.DELIVERED;
            }
            LOG.debug("Push-service accepted no message: {}", skippedReasons(response));
            return Attempt.REJECTED;
        } catch (RestClientResponseException e) {
            final HttpStatusCode status = e.getStatusCode();
            if (status.is4xxClientError()
                    && status.value() != HttpStatus.REQUEST_TIMEOUT.value()
                    && status.value() != HttpStatus.TOO_MANY_REQUESTS.value()) {
                LOG.warn("Push-service rejected the notification ({}): {}", status, e.getResponseBodyAsString());
                return Attempt.REJECTED;
            }
            LOG.warn("Push-service responded with {}, will retry", status);
            return Attempt.RETRY;
        } catch (RestClientException e) {
            LOG.warn("Could not reach the push-service ({}), will retry", e.getMessage());
            return Attempt.RETRY;
        }
    }

    private void enqueue(PendingPush push) {
        while (!queue.offerLast(push)) {
            if (queue.pollFirst() == null) {
                return;
            }
            LOG.warn("Push-notification queue is full ({}), dropped the oldest undelivered notification",
                    config.queueCapacity());
        }
        scheduleDrain(currentDelay);
    }

    private void drain() {
        drainScheduled.set(false);
        final PendingPush push = queue.pollFirst();
        if (push == null) {
            return;
        }
        if (push.attempts() >= config.maxAttempts()) {
            LOG.warn("Giving up on push-notification after {} attempt(s) (queued at {})",
                    push.attempts(), push.queuedAt());
            scheduleNextIfPending(currentDelay);
            return;
        }

        switch (attempt(push.request())) {
            case DELIVERED -> {
                currentDelay = config.initialDelay();
                scheduleNextIfPending(Duration.ZERO);
            }
            // The push-service is clearly reachable, so keep draining without backing off.
            case REJECTED -> {
                LOG.warn("Dropping push-notification rejected by the push-service (queued at {}, {} attempt(s))",
                        push.queuedAt(), push.attempts());
                scheduleNextIfPending(Duration.ZERO);
            }
            case RETRY -> {
                currentDelay = nextDelay(currentDelay);
                requeueHead(push.retried());
                scheduleDrain(currentDelay);
            }
        }
    }

    private void requeueHead(PendingPush push) {
        while (!queue.offerFirst(push)) {
            if (queue.pollLast() == null) {
                return;
            }
            LOG.warn("Push-notification queue is full ({}), dropped the newest undelivered notification",
                    config.queueCapacity());
        }
    }

    private void scheduleNextIfPending(Duration delay) {
        if (!queue.isEmpty()) {
            scheduleDrain(delay);
        }
    }

    private void scheduleDrain(Duration delay) {
        if (drainScheduled.compareAndSet(false, true)) {
            try {
                scheduler.schedule(this::drain, Math.max(0, delay.toMillis()), TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException e) {
                drainScheduled.set(false);
                LOG.warn("Push-notification retry-worker is shut down, {} notification(s) will not be delivered",
                        queue.size());
            }
        }
    }

    private Duration nextDelay(Duration delay) {
        final Duration next = Duration.ofMillis(Math.round(delay.toMillis() * config.multiplier()));
        return next.compareTo(config.maxDelay()) > 0 ? config.maxDelay() : next;
    }

    private static String skippedReasons(PushNotificationAccepted response) {
        final List<SkippedRecipient> skipped = response == null ? null : response.getSkipped();
        if (skipped == null || skipped.isEmpty()) {
            return "no recipients accepted";
        }
        return skipped.stream()
                .filter(Objects::nonNull)
                .map(s -> s.getRecipient() + ": " + s.getReason())
                .collect(Collectors.joining(", "));
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
        final int pending = queue.size();
        if (pending > 0) {
            LOG.warn("Shutting down with {} undelivered push-notification(s)", pending);
        }
    }

    private record PendingPush(PushNotificationRequest request, Instant queuedAt, int attempts) {
        PendingPush retried() {
            return new PendingPush(request, queuedAt, attempts + 1);
        }
    }
}
