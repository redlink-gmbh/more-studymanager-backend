package io.redlink.more.auth.token.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event to be thrown if a study participant is updated
 */
public class ParticipantUpdateEvent extends ApplicationEvent {
    private final Long studyId;
    private final Integer participantId;
    private final ParticipantUpdateAction action;

    public ParticipantUpdateEvent(Object source, Long studyId, Integer participantId, ParticipantUpdateAction action) {
        super(source);
        this.studyId = studyId;
        this.participantId = participantId;
        this.action = action;
    }

    public Long getStudyId() {
        return studyId;
    }

    public Integer getParticipantId() {
        return participantId;
    }

    public ParticipantUpdateAction getAction() {
        return action;
    }
}
