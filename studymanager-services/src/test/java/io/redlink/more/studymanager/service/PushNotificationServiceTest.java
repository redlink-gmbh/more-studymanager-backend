/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.service;

import io.redlink.more.studymanager.event.PushNotificationEvent;
import io.redlink.more.studymanager.push.client.model.MessageType;
import io.redlink.more.studymanager.push.client.model.Priority;
import io.redlink.more.studymanager.push.client.model.PushNotificationRequest;
import io.redlink.more.studymanager.push.client.model.Recipient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    PushNotificationDispatcher dispatcher;

    @InjectMocks
    PushNotificationService pushNotificationService;

    @Test
    void theEventDecidesTypeAndPriority() {
        pushNotificationService.handlePushNotification(new PushNotificationEvent(
                this, 7L, 42, "A Title", "A Message", Map.of("deepLink", "app://more/observation"),
                MessageType.FOREGROUND, Priority.URGENT));

        final PushNotificationRequest request = capturedRequest();
        assertThat(request.getType()).isEqualTo(MessageType.FOREGROUND);
        assertThat(request.getPriority()).isEqualTo(Priority.URGENT);
        assertThat(request.getTitle()).isEqualTo("A Title");
        assertThat(request.getBody()).isEqualTo("A Message");
        assertThat(request.getRecipients()).singleElement()
                .isEqualTo(new Recipient().studyId(7L).participantId(42));
        assertThat(request.getData())
                .containsEntry("deepLink", "app://more/observation");
    }

    @Test
    void backgroundMessagesCarryNoTitleOrBody() {
        pushNotificationService.handlePushNotification(new PushNotificationEvent(
                this, 7L, 42, "A Title", "A Message", Map.of("key", "MILESTONE_UPDATED"),
                MessageType.BACKGROUND, Priority.NORMAL));

        final PushNotificationRequest request = capturedRequest();
        assertThat(request.getType()).isEqualTo(MessageType.BACKGROUND);
        assertThat(request.getPriority()).isEqualTo(Priority.NORMAL);
        assertThat(request.getTitle()).isNull();
        assertThat(request.getBody()).isNull();
        assertThat(request.getData()).containsEntry("key", "MILESTONE_UPDATED");
    }

    @Test
    void everyParticipantOfOneEventIsSentInASingleRequest() {
        pushNotificationService.handlePushNotification(new PushNotificationEvent(
                this, 7L, List.of(42, 43, 44), null, null, Map.of(),
                MessageType.BACKGROUND, Priority.NORMAL));

        assertThat(capturedRequest().getRecipients())
                .map(Recipient::getParticipantId)
                .containsExactly(42, 43, 44);
        verify(dispatcher, times(1)).send(any());
    }

    @Test
    void anEventWithoutRecipientsIsNotSent() {
        pushNotificationService.handlePushNotification(new PushNotificationEvent(
                this, 7L, List.of(), "A Title", "A Message", Map.of(),
                MessageType.FOREGROUND, Priority.URGENT));

        verify(dispatcher, never()).send(any());
    }


    private PushNotificationRequest capturedRequest() {
        final ArgumentCaptor<PushNotificationRequest> request =
                ArgumentCaptor.forClass(PushNotificationRequest.class);
        verify(dispatcher).send(request.capture());
        return request.getValue();
    }
}
