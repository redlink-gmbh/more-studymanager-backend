package io.redlink.more.studymanager.model;

import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class GoalTemplate {
    private Long studyId;
    private Integer templateId;
    private String title;
    private String participantTitle;
    private String participantInfo;
    private String type;
    private String kind;
    private Integer studyGroupId;
    private GoalTemplateProperties properties;
    private Instant created;
    private Instant modified;
    private Set<Integer> observationGroupIds;
    private Set<String> topicKeys;
    private Set<Integer> adherenceCheckIds;

    public Long getStudyId() {
        return studyId;
    }

    public GoalTemplate setStudyId(Long studyId) {
        this.studyId = studyId;
        return this;
    }

    public Integer getTemplateId() {
        return templateId;
    }

    public GoalTemplate setTemplateId(Integer templateId) {
        this.templateId = templateId;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public GoalTemplate setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getParticipantTitle() {
        return participantTitle;
    }

    public GoalTemplate setParticipantTitle(String participantTitle) {
        this.participantTitle = participantTitle;
        return this;
    }

    public String getParticipantInfo() {
        return participantInfo;
    }

    public GoalTemplate setParticipantInfo(String participantInfo) {
        this.participantInfo = participantInfo;
        return this;
    }

    public String getType() {
        return type;
    }

    public GoalTemplate setType(String type) {
        this.type = type;
        return this;
    }

    public String getKind() {
        return kind;
    }

    public GoalTemplate setKind(String kind) {
        this.kind = kind;
        return this;
    }

    public Integer getStudyGroupId() {
        return studyGroupId;
    }

    public GoalTemplate setStudyGroupId(Integer studyGroupId) {
        this.studyGroupId = studyGroupId;
        return this;
    }

    public GoalTemplateProperties getProperties() {
        return properties;
    }

    public GoalTemplate setProperties(GoalTemplateProperties properties) {
        this.properties = properties;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public GoalTemplate setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public Instant getModified() {
        return modified;
    }

    public GoalTemplate setModified(Instant modified) {
        this.modified = modified;
        return this;
    }

    public GoalTemplate setObservationGroupIds(Set<Integer> observationGroupIds) {
        this.observationGroupIds = observationGroupIds == null ? new HashSet<>() : observationGroupIds;
        return this;
    }

    public Set<Integer> getObservationGroupIds() {
        return observationGroupIds;
    }

    public GoalTemplate setTopicKeys(Set<String> topicKeys) {
        this.topicKeys = topicKeys == null ? new HashSet<>() : topicKeys;
        return this;
    }

    public Set<String> getTopicKeys() {
        return topicKeys;
    }

    public GoalTemplate setAdherenceCheckIds(Set<Integer> adherenceCheckIds) {
        this.adherenceCheckIds = adherenceCheckIds == null ? new HashSet<>() : adherenceCheckIds;
        return this;
    }

    public Set<Integer> getAdherenceCheckIds() {
        return adherenceCheckIds;
    }
}