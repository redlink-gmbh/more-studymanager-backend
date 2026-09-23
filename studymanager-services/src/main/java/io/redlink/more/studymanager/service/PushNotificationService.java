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
import io.redlink.more.studymanager.push.client.model.PushNotificationRequest;
import io.redlink.more.studymanager.push.client.model.Recipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Delivers the push-notifications the rest of the application asks for.
 * <p>
 * Every publisher specifies its own notification, so this service only translates a
 * {@link PushNotificationEvent} into a call to the push-service. Retrying while the push-service
 * is unreachable is the {@link PushNotificationDispatcher}'s job; the notifications themselves are
 * stored by the push-service, not here.
 */
@Service
public class PushNotificationService {
    private static final Logger LOG = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushNotificationDispatcher dispatcher;

    public PushNotificationService(PushNotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventListener
    public void handlePushNotification(PushNotificationEvent event) {
        if (event.getParticipantIds().isEmpty()) {
            return;
        }
        LOG.debug("Sending {} {} notification to {} participant(s) of study {}",
                event.getPriority(), event.getType(), event.getParticipantIds().size(), event.getStudyId());

        final Map<String, String> data = new HashMap<>(event.getData());
        data.put("MSG_ID", UUID.randomUUID().toString());

        final PushNotificationRequest request = new PushNotificationRequest()
                .data(data)
                .type(event.getType())
                .priority(event.getPriority());

        event.getParticipantIds().forEach(participantId -> request.addRecipientsItem(
                new Recipient().studyId(event.getStudyId()).participantId(participantId)));

        // A BACKGROUND message is data-only: the app handles it itself and the OS renders nothing,
        // so title and body would only be dead weight on the wire.
        if (event.getType() != MessageType.BACKGROUND && event.getTitle() != null && event.getMessage() != null) {
            request.title(event.getTitle()).body(event.getMessage());
        }

        if (dispatcher.send(request) == PushNotificationDispatcher.Outcome.REJECTED) {
            LOG.debug("Could not send notification to participant(s) {} of study {}",
                    event.getParticipantIds(), event.getStudyId());
        }
    }
}
