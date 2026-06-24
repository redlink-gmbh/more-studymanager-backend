package io.redlink.more.studymanager.model;

import java.util.ArrayList;
import java.util.List;

public class StudyGoalConfigData extends StudyGoalConfig {

    private List<GoalTopic> topics =  new ArrayList<>();
    private List<GoalAdherenceCheck> adherenceChecks = new ArrayList<>();

    public StudyGoalConfigData(long studyId) {
        super();
        setStudyId(studyId);
    }

    public StudyGoalConfigData(StudyGoalConfig config) {
        super();
        setStudyId(config.getStudyId());
        setAchievability(config.getAchievability());
        setCommitment(config.getCommitment());
        setUnderstandability(config.getUnderstandability());
    }

    public List<GoalAdherenceCheck> getAdherenceChecks() {
        return adherenceChecks;
    }

    public StudyGoalConfigData setAdherenceChecks(List<GoalAdherenceCheck> adherenceChecks) {
        this.adherenceChecks = adherenceChecks == null ? new ArrayList<>() : adherenceChecks;
        return this;
    }

    public List<GoalTopic> getTopics() {
        return topics;
    }

    public StudyGoalConfigData setTopics(List<GoalTopic> topics) {
        this.topics = topics == null ? new ArrayList<>() : topics;
        return this;
    }

    @Override
    public StudyGoalConfigData setAchievability(String achievability) {
        super.setAchievability(achievability);
        return this;
    }

    @Override
    public StudyGoalConfigData setCommitment(String commitment) {
        super.setCommitment(commitment);
        return this;
    }

    @Override
    public StudyGoalConfigData setStudyId(Long studyId) {
        super.setStudyId(studyId);
        return this;
    }

    @Override
    public StudyGoalConfigData setUnderstandability(String understandability) {
        super.setUnderstandability(understandability);
        return this;
    }

}
