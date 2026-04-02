package io.redlink.more.studymanager.model;

import io.redlink.more.studymanager.core.properties.GoalProperties;

import java.time.Instant;

public class Goal {
    private Long studyId;
    private Integer goalId;
    private Integer participantId;
    private Integer templateId;
    private GoalProperties properties;
    private Instant created;
    private Instant modified;

    public Long getStudyId() {
        return studyId;
    }

    public Goal setStudyId(Long studyId) {
        this.studyId = studyId;
        return this;
    }

    public Integer getGoalId() {
        return goalId;
    }

    public Goal setGoalId(Integer goalId) {
        this.goalId = goalId;
        return this;
    }

    public Integer getParticipantId() {
        return participantId;
    }

    public Goal setParticipantId(Integer participantId) {
        this.participantId = participantId;
        return this;
    }

    public Integer getTemplateId() {
        return templateId;
    }

    public Goal setTemplateId(Integer templateId) {
        this.templateId = templateId;
        return this;
    }

    public GoalProperties getProperties() {
        return properties;
    }

    public Goal setProperties(GoalProperties properties) {
        this.properties = properties;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public Goal setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public Instant getModified() {
        return modified;
    }

    public Goal setModified(Instant modified) {
        this.modified = modified;
        return this;
    }
}
