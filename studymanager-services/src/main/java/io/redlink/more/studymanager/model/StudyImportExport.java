/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StudyImportExport {

    private Study study;
    private List<StudyGroup> studyGroups = new ArrayList<>();
    private List<ObservationGroup> observationGroups = new ArrayList<>();
    private List<Observation> observations = new ArrayList<>();
    private List<Intervention> interventions = new ArrayList<>();
    private List<Milestone> milestones = new ArrayList<>();
    private List<ParticipantInfo> participants = new ArrayList<>();
    private Map<Integer, Trigger> triggers = new HashMap<>();
    private Map<Integer, List<Action>> actions = new HashMap<>();
    private List<IntegrationInfo> integrations = new ArrayList<>();
    private StudyGoalConfigData studyGoalConfig = null;
    private List<GoalTemplate> goalTemplates = new ArrayList<>();

    public Study getStudy() {
        return study;
    }

    public StudyImportExport setStudy(Study study) {
        this.study = study;
        return this;
    }

    public List<StudyGroup> getStudyGroups() {
        return studyGroups;
    }

    public StudyImportExport setStudyGroups(List<StudyGroup> studyGroups) {
        this.studyGroups = studyGroups == null ? new ArrayList<>() : studyGroups;
        return this;
    }

    public List<ObservationGroup> getObservationGroups() {
        return observationGroups;
    }

    public StudyImportExport setObservationGroups(List<ObservationGroup> observationGroups) {
        this.observationGroups = observationGroups == null ? new ArrayList<>() : observationGroups;
        return this;
    }

    public List<Observation> getObservations() {
        return observations;
    }

    public StudyImportExport setObservations(List<Observation> observations) {
        this.observations = observations == null ? new ArrayList<>() : observations;
        return this;
    }

    public List<Intervention> getInterventions() {
        return interventions;
    }

    public StudyImportExport setInterventions(List<Intervention> interventions) {
        this.interventions = interventions == null ? new ArrayList<>() : interventions;
        return this;
    }

    public List<Milestone> getMilestones() {
        return milestones;
    }

    public StudyImportExport setMilestones(List<Milestone> milestones) {
        this.milestones = milestones == null ? new ArrayList<>() : milestones;
        return this;
    }

    public Map<Integer, Trigger> getTriggers() {
        return triggers;
    }

    public StudyImportExport setTriggers(Map<Integer, Trigger> triggers) {
        this.triggers = triggers == null ? new HashMap<>() : triggers;
        return this;
    }

    public Map<Integer, List<Action>> getActions() {
        return actions;
    }

    public StudyImportExport setActions(Map<Integer, List<Action>> actions) {
        this.actions = actions == null ? new HashMap<>() : actions;
        return this;
    }

    public List<ParticipantInfo> getParticipants() {
        return participants;
    }

    public StudyImportExport setParticipants(List<ParticipantInfo> participants) {
        this.participants = participants == null ? new ArrayList<>() : participants;
        return this;
    }

    public List<IntegrationInfo> getIntegrations() {
        return integrations;
    }

    public StudyImportExport setIntegrations(List<IntegrationInfo> integrations) {
        this.integrations = integrations == null ? new ArrayList<>() : integrations;
        return this;
    }

    public List<GoalTemplate> getGoalTemplates() {
        return goalTemplates;
    }

    public StudyImportExport setGoalTemplates(List<GoalTemplate> goalTemplates) {
        this.goalTemplates = goalTemplates == null ? new ArrayList<>() : goalTemplates;
        return this;
    }

    public StudyGoalConfigData getStudyGoalConfig() {
        return studyGoalConfig;
    }

    public StudyImportExport setStudyGoalConfig(StudyGoalConfigData studyGoalConfig) {
        this.studyGoalConfig = studyGoalConfig;
        return this;
    }

    public record ParticipantInfo(
            Integer groupId,
            Set<Integer> observationGroupIds,
            List<ParticipantMilestoneInfo> milestones
    ) {
        public ParticipantInfo {
            observationGroupIds = observationGroupIds == null ? Collections.emptySet() : observationGroupIds;
            milestones = milestones == null ? Collections.emptyList() : milestones;
        }

        public ParticipantInfo(Integer groupId, Set<Integer> observationGroupIds) {
            this(groupId, observationGroupIds, null);
        }
    }

    public static class StudyGoalConfigData extends StudyGoalConfig {

        private List<GoalTopic> topics = new ArrayList<>();
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

        public StudyGoalConfigData setAdherenceChecks(Collection<GoalAdherenceCheck> adherenceChecks) {
            this.adherenceChecks = adherenceChecks == null ? new ArrayList<>() : new ArrayList<>(adherenceChecks);
            return this;
        }

        public List<GoalTopic> getTopics() {
            return topics;
        }

        public StudyGoalConfigData setTopics(Collection<GoalTopic> topics) {
            this.topics = topics == null ? new ArrayList<>() : new ArrayList<>(topics);
            return this;
        }

    }
}
