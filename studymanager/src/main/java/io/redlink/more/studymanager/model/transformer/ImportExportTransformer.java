/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.model.transformer;

import io.redlink.more.studymanager.api.v1.model.AdherenceCheckScheduleEnumDTO;
import io.redlink.more.studymanager.api.v1.model.GoalAdherenceCheckDTO;
import io.redlink.more.studymanager.api.v1.model.GoalConfigurationDTO;
import io.redlink.more.studymanager.api.v1.model.GoalConsentDTO;
import io.redlink.more.studymanager.api.v1.model.GoalTemplateCategoriesDTO;
import io.redlink.more.studymanager.api.v1.model.GoalTemplateDTO;
import io.redlink.more.studymanager.api.v1.model.GoalTopicDTO;
import io.redlink.more.studymanager.api.v1.model.IntegrationInfoDTO;
import io.redlink.more.studymanager.api.v1.model.InterventionDTO;
import io.redlink.more.studymanager.api.v1.model.ParticipantInfoDTO;
import io.redlink.more.studymanager.api.v1.model.StudyImportExportDTO;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.model.GoalAdherenceCheck;
import io.redlink.more.studymanager.model.GoalTemplate;
import io.redlink.more.studymanager.model.GoalTopic;
import io.redlink.more.studymanager.model.IntegrationInfo;
import io.redlink.more.studymanager.model.StudyImportExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ImportExportTransformer {

    private static final Logger log = LoggerFactory.getLogger(ImportExportTransformer.class);

    private ImportExportTransformer() {}

    public static StudyImportExport fromStudyImportExportDTO_V1(StudyImportExportDTO dto) {
        return new StudyImportExport()
                .setStudy(StudyTransformer.fromStudyDTO_V1(dto.getStudy()))
                .setStudyGroups(transform(dto.getStudyGroups(), StudyGroupTransformer::fromStudyGroupDTO_V1))
                .setObservationGroups(transform(dto.getObservationGroups(), ObservationGroupTransformer::fromObservationGroupDTO_V1))
                .setObservations(transform(dto.getObservations(), ObservationTransformer::fromObservationDTO_V1))
                .setInterventions(transform(dto.getInterventions(), InterventionTransformer::fromInterventionDTO_V1))
                .setTriggers(
                        dto.getInterventions().stream().collect(Collectors.toMap(
                                InterventionDTO::getInterventionId,
                                interventionDTO ->
                                        TriggerTransformer.fromTriggerDTO_V1(interventionDTO.getTrigger())
                        ))
                )
                .setActions(
                        dto.getInterventions().stream().collect(Collectors.toMap(
                                InterventionDTO::getInterventionId,
                                interventionDTO ->
                                        transform(interventionDTO.getActions(), ActionTransformer::fromActionDTO_V1)
                        ))
                )
                .setParticipants(transform(dto.getParticipants(), ImportExportTransformer::fromParticipantDTO_V1))
                .setIntegrations(transform(dto.getIntegrations(), ImportExportTransformer::fromIntegrationExportDTO_V1))
                .setStudyGoalConfig(fromStudyGoalConfigDTO_V1(dto.getStudy().getStudyId(), dto.getGoalConfiguration()))
                .setGoalTemplates(transform(dto.getGoalTemplates(), ImportExportTransformer::fromGoalTemplateDTO_V1));
    }

    private static GoalTemplate fromGoalTemplateDTO_V1(GoalTemplateDTO dto) {
        return new GoalTemplate()
                .setStudyId(dto.getStudyId())
                .setTemplateId(dto.getTemplateId())
                .setType(dto.getType())
                .setKind(dto.getCategories().getKind().getValue())
                .setTopicKeys(Set.copyOf(dto.getCategories().getTopics()))
                .setAdherenceCheckIds(dto.getAdherenceChecks().stream().map(AdherenceCheckScheduleEnumDTO::ordinal).collect(Collectors.toSet()))
                .setObservationGroupIds(dto.getObservationGroupIds())
                .setStudyGroupId(dto.getStudyGroupId())
                .setTitle(dto.getTitle())
                .setParticipantTitle(dto.getParticipantTitle())
                .setParticipantInfo(dto.getParticipantInfo())
                .setProperties(new GoalTemplateProperties(dto.getProperties()));


    }

    private static StudyImportExport.StudyGoalConfigData fromStudyGoalConfigDTO_V1(long studyId, GoalConfigurationDTO dto) {
        var config = new StudyImportExport.StudyGoalConfigData(studyId);
        config.setAchievability(dto.getConsent() == null ? null : dto.getConsent().getAchievability())
                .setCommitment(dto.getConsent() == null ? null : dto.getConsent().getCommitment())
                .setUnderstandability(dto.getConsent() == null ? null : dto.getConsent().getUnderstandability());
        config.setTopics(transform(dto.getTopics(), gtDto -> fromGoalTopicDTO_V1(studyId, gtDto)));
        config.setAdherenceChecks(transform(dto.getAdherenceChecks(), acDto -> fromAdherenceCheckDTO_V1(studyId, acDto)));
        return config;
    }

    private static GoalAdherenceCheck fromAdherenceCheckDTO_V1(long studyId, GoalAdherenceCheckDTO dto) {
        return new GoalAdherenceCheck()
                .setStudyId(studyId)
                .setCheckId(dto.getCheck().ordinal())
                .setTitle(dto.getCheck().getValue())
                .setTime(dto.getTime());
    }

    private static GoalTopic fromGoalTopicDTO_V1(long studyId, GoalTopicDTO dto) {
        return new GoalTopic()
                .setStudyId(studyId)
                .setKey(dto.getKey())
                .setTitle(dto.getTitle())
                .setDescription(dto.getDescription());
    }

    public static StudyImportExportDTO toStudyImportExportDTO_V1(StudyImportExport studyImportExport) {
        return new StudyImportExportDTO()
                .study(StudyTransformer.toStudyDTO_V1(studyImportExport.getStudy()))
                .studyGroups(transform(studyImportExport.getStudyGroups(), StudyGroupTransformer::toStudyGroupDTO_V1))
                .observationGroups(transform(studyImportExport.getObservationGroups(), ObservationGroupTransformer::toObservationGroupDTO_V1))
                .observations(transform(studyImportExport.getObservations(), ObservationTransformer::toObservationDTO_V1))
                .interventions(transform(studyImportExport.getInterventions(), intervention ->
                    InterventionTransformer.toInterventionDTO_V1(intervention)
                            .trigger(
                                    TriggerTransformer.toTriggerDTO_V1(
                                            studyImportExport.getTriggers().get(intervention.getInterventionId())
                                    )
                            )
                            .actions(
                                    transform(
                                            studyImportExport.getActions().get(intervention.getInterventionId()),
                                            ActionTransformer::toActionDTO_V1
                                    )
                            )
                ))
                .participants(transform(studyImportExport.getParticipants(), ImportExportTransformer::toParticipantDTO_V1))
                .integrations(transform(studyImportExport.getIntegrations(), ImportExportTransformer::toIntegrationInfoDTO_V1))
                .goalConfiguration(toGoalConfigurationDTO_V1(studyImportExport.getStudyGoalConfig()))
                .goalTemplates(transform(studyImportExport.getGoalTemplates(), ImportExportTransformer::toGoalTemplateDTO_V1));
    }

    private static GoalConfigurationDTO toGoalConfigurationDTO_V1(StudyImportExport.StudyGoalConfigData goalConfig){
        return new GoalConfigurationDTO()
                .consent(new GoalConsentDTO()
                        .achievability(goalConfig.getAchievability())
                        .commitment(goalConfig.getCommitment())
                        .understandability(goalConfig.getUnderstandability()))
                .topics(transform(goalConfig.getTopics(), ImportExportTransformer::toGoalTopicDTO_V1))
                .adherenceChecks(transform(goalConfig.getAdherenceChecks(), ImportExportTransformer::goalAdherenceCheckDTO_V1));
    }

    private static GoalTopicDTO toGoalTopicDTO_V1(GoalTopic goalTopic){
        return new GoalTopicDTO()
                .title(goalTopic.getTitle())
                .key(goalTopic.getKey())
                .description(goalTopic.getDescription());
    }

    private static GoalTemplateDTO toGoalTemplateDTO_V1(GoalTemplate goalTemplate){
        return new GoalTemplateDTO()
                .studyId(goalTemplate.getStudyId())
                .title(goalTemplate.getTitle())
                .templateId(goalTemplate.getTemplateId())
                .adherenceChecks(transform(goalTemplate.getAdherenceCheckIds(), ImportExportTransformer::toAdherenceCheckEnumDTO_V1))
                .categories(toGoalTemplateCategoriesDTO_V1(goalTemplate))
                .observationGroupIds(goalTemplate.getObservationGroupIds())
                .studyGroupId(goalTemplate.getStudyGroupId())
                .type(goalTemplate.getType())
                .participantInfo(goalTemplate.getParticipantInfo())
                .participantTitle(goalTemplate.getParticipantTitle())
                .properties(goalTemplate.getProperties());
    }

    private static GoalTemplateCategoriesDTO toGoalTemplateCategoriesDTO_V1(GoalTemplate goalTemplate){
        return new GoalTemplateCategoriesDTO()
                .kind(GoalTemplateCategoriesDTO.KindEnum.fromValue(goalTemplate.getKind()))
                .topics(goalTemplate.getTopicKeys() == null ? null : List.copyOf(goalTemplate.getTopicKeys()));
    }

    private static AdherenceCheckScheduleEnumDTO toAdherenceCheckEnumDTO_V1(Integer adherenceCheckId){
        return AdherenceCheckScheduleEnumDTO.values()[adherenceCheckId];
    }

    private static GoalAdherenceCheckDTO goalAdherenceCheckDTO_V1(GoalAdherenceCheck goalAdherenceCheck){
        return new GoalAdherenceCheckDTO()
                .check(AdherenceCheckScheduleEnumDTO.values()[goalAdherenceCheck.getCheckId()]) //we store the ordinal as checkID
                .time(goalAdherenceCheck.getTime());
    }

    private static ParticipantInfoDTO toParticipantDTO_V1(StudyImportExport.ParticipantInfo participant) {
        return new ParticipantInfoDTO()
                .studyGroup(participant.groupId())
                .observationGroups(participant.observationGroupIds());
    }

    private static StudyImportExport.ParticipantInfo fromParticipantDTO_V1(ParticipantInfoDTO participant) {
        return new StudyImportExport.ParticipantInfo(
                participant.getStudyGroup(),
                participant.getObservationGroups() == null ? Collections.emptySet() : participant.getObservationGroups());
    }

    private static <S, T> List<T> transform(Collection<S> elements, Function<S, T> transformer) {
        if (elements == null) { return List.of(); }
        return elements.stream().map(transformer).toList();
    }

    private static IntegrationInfoDTO toIntegrationInfoDTO_V1(IntegrationInfo integration) {
        return new IntegrationInfoDTO()
                .name(integration.name())
                .observationId(integration.observationId());
    }

    private static IntegrationInfo fromIntegrationExportDTO_V1(IntegrationInfoDTO integration) {
        return new IntegrationInfo(integration.getName(), integration.getObservationId());
    }
}
