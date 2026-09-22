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
import io.redlink.more.studymanager.push.client.model.Recipient;
import io.redlink.more.studymanager.push.client.model.SkippedRecipient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotificationDispatcherTest {

    @Mock
    NotificationsApi notificationsApi;

    @Mock
    ScheduledExecutorService scheduler;

    /** The delays the retry-worker was scheduled with, so the backoff itself can be asserted. */
    private final List<Duration> delays = new ArrayList<>();
    private final List<Runnable> pending = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Run the retry-worker on demand instead of after a delay.
        lenient().when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .thenAnswer(invocation -> {
                    final TimeUnit unit = invocation.getArgument(2);
                    delays.add(Duration.ofMillis(unit.toMillis(invocation.getArgument(1, Long.class))));
                    pending.add(invocation.getArgument(0));
                    return null;
                });
    }

    private void runPending() {
        final List<Runnable> due = List.copyOf(pending);
        pending.clear();
        due.forEach(Runnable::run);
    }

    @Test
    void acceptedNotificationIsDeliveredWithoutQueueing() {
        when(notificationsApi.sendNotification(any())).thenReturn(accepted(1));

        final PushNotificationDispatcher.Outcome outcome =
                dispatcher(defaults()).send(request());

        assertThat(outcome).isEqualTo(PushNotificationDispatcher.Outcome.DELIVERED);
        assertThat(delays).isEmpty();
    }

    @Test
    void notificationWithoutAcceptedRecipientIsRejected() {
        when(notificationsApi.sendNotification(any())).thenReturn(
                accepted(0).addSkippedItem(new SkippedRecipient()
                        .recipient(new Recipient().studyId(7L).participantId(42))
                        .reason("NO_TOKEN_REGISTERED")));

        final PushNotificationDispatcher.Outcome outcome =
                dispatcher(defaults()).send(request());

        assertThat(outcome).isEqualTo(PushNotificationDispatcher.Outcome.REJECTED);
        assertThat(delays).isEmpty();
    }

    @Test
    void badRequestIsRejectedWithoutRetry() {
        when(notificationsApi.sendNotification(any()))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null, null, null));

        final PushNotificationDispatcher.Outcome outcome =
                dispatcher(defaults()).send(request());

        assertThat(outcome).isEqualTo(PushNotificationDispatcher.Outcome.REJECTED);
        assertThat(delays).isEmpty();
    }

    @Test
    void unreachableServiceQueuesTheNotificationAndDeliversItOnRetry() {
        when(notificationsApi.sendNotification(any()))
                .thenThrow(new ResourceAccessException("Connection refused"))
                .thenReturn(accepted(1));

        final PushNotificationDispatcher dispatcher = dispatcher(defaults());
        final PushNotificationDispatcher.Outcome outcome = dispatcher.send(request());

        assertThat(outcome).isEqualTo(PushNotificationDispatcher.Outcome.QUEUED);
        assertThat(delays).containsExactly(Duration.ofSeconds(5));

        runPending();

        verify(notificationsApi, times(2)).sendNotification(any());
        // nothing left to drain
        assertThat(delays).containsExactly(Duration.ofSeconds(5));
    }

    @Test
    void backoffGrowsPerFailureAndIsCappedAtMaxDelay() {
        when(notificationsApi.sendNotification(any())).thenThrow(new ResourceAccessException("down"));

        final PushNotificationDispatcher dispatcher = dispatcher(defaults());
        dispatcher.send(request());
        for (int i = 0; i < 5; i++) {
            runPending();
        }

        assertThat(delays).containsExactly(
                Duration.ofSeconds(5),
                Duration.ofSeconds(10),
                Duration.ofSeconds(20),
                Duration.ofSeconds(30),  // capped
                Duration.ofSeconds(30),
                Duration.ofSeconds(30));
    }

    @Test
    void backoffIsResetAfterASuccessfulDelivery() {
        when(notificationsApi.sendNotification(any()))
                .thenThrow(new ResourceAccessException("down"))
                .thenThrow(new ResourceAccessException("down"))
                .thenReturn(accepted(1))
                .thenThrow(new ResourceAccessException("down again"));

        final PushNotificationDispatcher dispatcher = dispatcher(defaults());
        dispatcher.send(request());     // queued, scheduled at 5s
        runPending();                    // fails, scheduled at 10s
        runPending();                    // delivered, backoff reset
        dispatcher.send(request());     // fails again, queued

        assertThat(delays).containsExactly(
                Duration.ofSeconds(5),
                Duration.ofSeconds(10),
                Duration.ofSeconds(5));
    }

    @Test
    void notificationIsDroppedAfterMaxAttempts() {
        when(notificationsApi.sendNotification(any())).thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY));

        final PushNotificationDispatcher dispatcher = dispatcher(retry(3, 100));
        dispatcher.send(request());     // attempt 1
        runPending();                    // attempt 2
        runPending();                    // attempt 3, gives up
        runPending();                    // queue is empty, nothing scheduled

        verify(notificationsApi, times(3)).sendNotification(any());
        verifyNoMoreInteractions(notificationsApi);
    }

    @Test
    void fullQueueDropsTheOldestNotification() {
        when(notificationsApi.sendNotification(any()))
                .thenThrow(new ResourceAccessException("down"))
                .thenThrow(new ResourceAccessException("down"))
                .thenReturn(accepted(1));
        final PushNotificationDispatcher dispatcher = dispatcher(retry(10, 1));
        dispatcher.send(requestFor(42));
        dispatcher.send(requestFor(43));

        runPending();

        final ArgumentCaptor<PushNotificationRequest> sent =
                ArgumentCaptor.forClass(PushNotificationRequest.class);
        verify(notificationsApi, times(3)).sendNotification(sent.capture());

        final List<PushNotificationRequest> sentRequests = sent.getAllValues();
        assertThat(sentRequests.get(sentRequests.size() - 1).getRecipients().get(0).getParticipantId())
                .as("the notification that stayed in the queue, the older one was evicted")
                .isEqualTo(43);
    }

    private PushNotificationDispatcher dispatcher(PushServiceProperties props) {
        return new PushNotificationDispatcher(notificationsApi, props, scheduler);
    }

    private static PushServiceProperties defaults() {
        return properties(new PushServiceProperties.Retry(
                Duration.ofSeconds(5), 2, Duration.ofSeconds(30), 10, 1000));
    }

    private static PushServiceProperties retry(int maxAttempts, int queueCapacity) {
        return properties(new PushServiceProperties.Retry(
                Duration.ofSeconds(5), 2, Duration.ofSeconds(30), maxAttempts, queueCapacity));
    }

    private static PushServiceProperties properties(PushServiceProperties.Retry retry) {
        return new PushServiceProperties(URI.create("http://localhost:8090"), "a-key",
                Duration.ofSeconds(5), Duration.ofSeconds(10), retry);
    }

    private static PushNotificationRequest request() {
        return requestFor(42);
    }

    private static PushNotificationRequest requestFor(int participantId) {
        return new PushNotificationRequest()
                .addRecipientsItem(new Recipient().studyId(7L).participantId(participantId));
    }

    private static PushNotificationAccepted accepted(int count) {
        return new PushNotificationAccepted().batchId(UUID.randomUUID()).accepted(count);
    }
}
