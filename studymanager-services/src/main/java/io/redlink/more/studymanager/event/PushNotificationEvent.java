/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.event;

import io.redlink.more.studymanager.push.client.model.MessageType;
import io.redlink.more.studymanager.push.client.model.Priority;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.Map;

/**
 * A push-notification to deliver, fully specified by whoever wants it sent: every publisher picks
 * its own recipients, payload, message-type and priority.
 * <p>
 * All participants listed here receive the same message in a single call to the push-service.
 */
public class PushNotificationEvent extends ApplicationEvent {

    private final long studyId;
    private final List<Integer> participantIds;
    private final String title;
    private final String message;
    private final Map<String, String> data;
    private final MessageType type;
    private final Priority priority;

    public PushNotificationEvent(Object source, long studyId, int participantId,
                                 String title, String message, Map<String, String> data,
                                 MessageType type, Priority priority) {
        this(source, studyId, List.of(participantId), title, message, data, type, priority);
    }

    public PushNotificationEvent(Object source, long studyId, List<Integer> participantIds,
                                 String title, String message, Map<String, String> data,
                                 MessageType type, Priority priority) {
        super(source);
        this.studyId = studyId;
        this.participantIds = List.copyOf(participantIds);
        this.title = title;
        this.message = message;
        this.data = data == null ? Map.of() : Map.copyOf(data);
        this.type = type;
        this.priority = priority;
    }

    public long getStudyId() {
        return studyId;
    }

    public List<Integer> getParticipantIds() {
        return participantIds;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getData() {
        return data;
    }

    public MessageType getType() {
        return type;
    }

    public Priority getPriority() {
        return priority;
    }
}
