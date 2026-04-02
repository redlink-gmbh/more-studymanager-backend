package io.redlink.more.studymanager.model;

import java.time.Instant;

public class GoalTopic {
    private Long studyId;
    private String key;
    private String title;
    private String description;
    private Instant created;
    private Instant modified;

    public Long getStudyId() {
        return studyId;
    }

    public GoalTopic setStudyId(Long studyId) {
        this.studyId = studyId;
        return this;
    }

    public String getKey() {
        return key;
    }

    public GoalTopic setKey(String key) {
        this.key = key;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public GoalTopic setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public GoalTopic setDescription(String description) {
        this.description = description;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public GoalTopic setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public Instant getModified() {
        return modified;
    }

    public GoalTopic setModified(Instant modified) {
        this.modified = modified;
        return this;
    }
}