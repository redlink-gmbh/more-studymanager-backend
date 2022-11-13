package io.redlink.more.studymanager.model;
import java.sql.Timestamp;

public class Participant {
    private Long studyId;
    private Integer participantId;
    private String alias;
    private Integer studyGroupId;
    private Status status;
    private Timestamp created;
    private Timestamp modified;

    private String registrationToken;

    public Long getStudyId() {
        return this.studyId;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        NEW("new"),
        ACTIVE("active"),
        ABANDONED("abandoned"),
        KICKED_OUT("kicked_out"),
        LOCKED("locked");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public Participant setStatus(Status status) {
        this.status = status;
        return this;
    }

    public Participant setStudyId(Long studyId) {
        this.studyId = studyId;
        return this;
    }

    public Integer getParticipantId() {
        return participantId;
    }

    public Participant setParticipantId(Integer participantId) {
        this.participantId = participantId;
        return this;
    }

    public String getAlias() {
        return alias;
    }

    public Participant setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public Integer getStudyGroupId() {
        return studyGroupId;
    }

    public Participant setStudyGroupId(Integer studyGroupId) {
        this.studyGroupId = studyGroupId;
        return this;
    }

    public Timestamp getCreated() {
        return created;
    }

    public Participant setCreated(Timestamp created) {
        this.created = created;
        return this;
    }

    public Timestamp getModified() {
        return modified;
    }

    public Participant setModified(Timestamp modified) {
        this.modified = modified;
        return this;
    }

    public String getRegistrationToken() {
        return registrationToken;
    }

    public Participant setRegistrationToken(String registrationToken) {
        this.registrationToken = registrationToken;
        return this;
    }
}