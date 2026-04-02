package io.redlink.more.studymanager.model;

public class StudyGoalConfig {
    private Long studyId;
    private String commitment;
    private String achievability;
    private String understandability;

    public Long getStudyId() {
        return studyId;
    }

    public StudyGoalConfig setStudyId(Long studyId) {
        this.studyId = studyId;
        return this;
    }

    public String getCommitment() {
        return commitment;
    }

    public StudyGoalConfig setCommitment(String commitment) {
        this.commitment = commitment;
        return this;
    }

    public String getAchievability() {
        return achievability;
    }

    public StudyGoalConfig setAchievability(String achievability) {
        this.achievability = achievability;
        return this;
    }

    public String getUnderstandability() {
        return understandability;
    }

    public StudyGoalConfig setUnderstandability(String understandability) {
        this.understandability = understandability;
        return this;
    }
}