/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.model.transformer;

import io.redlink.more.studymanager.api.v1.model.*;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.model.GoalAdherenceCheck;
import io.redlink.more.studymanager.model.GoalTemplate;
import io.redlink.more.studymanager.model.GoalTopic;
import io.redlink.more.studymanager.model.StudyGoalConfig;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class GoalV1Transformer {

    private GoalV1Transformer() {}

// ========================== MODEL → DTO ==========================

    public static StudyGoalConfigDataDTO toStudyGoalConfigDataDTO_V1(
            StudyGoalConfig config,
            Collection<GoalTopic> topics,
            Collection<GoalAdherenceCheck> checks) {

        final StudyGoalConfigConsentsDTO consents = toStudyGoalConfigConsentsDTO_V1(config);

        final List<StudyGoalConfigScheduleInnerDTO> schedule = checks.stream()
                .map(GoalV1Transformer::toStudyGoalConfigScheduleInnerDTO_V1)
                .toList();

        final List<GoalTopicDTO> topicDtos = topics.stream()
                .map(GoalV1Transformer::toGoalTopicDTO_V1)
                .toList();

        return new StudyGoalConfigDataDTO()
                .consents(consents)
                .schedule(schedule)
                .topics(topicDtos);
    }

    public static StudyGoalConfigConsentsDTO toStudyGoalConfigConsentsDTO_V1(StudyGoalConfig config) {
        return new StudyGoalConfigConsentsDTO()
                .commitment(config.getCommitment())
                .achievability(config.getAchievability())
                .understandable(config.getUnderstandability());
    }

    public static StudyGoalConfigScheduleInnerDTO toStudyGoalConfigScheduleInnerDTO_V1(GoalAdherenceCheck check) {
        return new StudyGoalConfigScheduleInnerDTO()
                .key(mapTitleToScheduleEnum(check.getTitle()))
                .time(check.getTime());           // LocalTime → LocalTime (no conversion needed)
    }

    public static GoalTopicDTO toGoalTopicDTO_V1(GoalTopic topic) {
        return new GoalTopicDTO()
                .key(topic.getKey())
                .title(topic.getTitle())
                .description(topic.getDescription());
    }

    public static GoalTemplateDTO toGoalTemplateDTO_V1(GoalTemplate template) {
        if (template == null) return null;

        GoalTemplateCategoriesDTO categories = new GoalTemplateCategoriesDTO()
                .kind(mapKindToEnum(template.getKind()))
                .topics(template.getTopicKeys() != null ? template.getTopicKeys().stream().toList() : List.of());

        List<AdherenceCheckScheduleEnumDTO> adherenceChecks = template.getAdherenceCheckIds() != null
                ? template.getAdherenceCheckIds().stream()
                .map(GoalV1Transformer::mapOrdinalToAdherenceEnum) // if needed, otherwise you may store the enum value directly
                .filter(Objects::nonNull)
                .toList()
                : List.of();

        return new GoalTemplateDTO()
                .studyId(template.getStudyId())
                .templateId(template.getTemplateId())
                .studyGroupId(template.getStudyGroupId())
                .observationGroupIds(template.getObservationGroupIds() != null ? template.getObservationGroupIds() : Set.of())
                .title(template.getTitle())
                .participantTitle(template.getParticipantTitle())
                .participantInfo(template.getParticipantInfo())
                .type(template.getType())
                .categories(categories)
                .adherenceChecks(adherenceChecks)
                .properties(template.getProperties() != null ? template.getProperties() : Map.of())
                .created(template.getCreated())
                .modified(template.getModified());
    }

    // ========================== DTO → MODEL ==========================

    public static StudyGoalConfig toStudyGoalConfig(StudyGoalConfigDTO dto, Long studyId) {
        if (dto == null) return null;

        return new StudyGoalConfig()
                .setStudyId(studyId)
                .setCommitment(dto.getConsents() != null ? dto.getConsents().getCommitment() : null)
                .setAchievability(dto.getConsents() != null ? dto.getConsents().getAchievability() : null)
                .setUnderstandability(dto.getConsents() != null ? dto.getConsents().getUnderstandable() : null);
    }

    public static List<GoalTopic> toGoalTopics(StudyGoalConfigDataDTO dto, Long studyId) {
        if (dto == null || dto.getTopics() == null) return List.of();

        return dto.getTopics().stream()
                .filter(Objects::nonNull)
                .map(t -> toGoalTopic(t, studyId))
                .toList();
    }

    public static GoalTopic toGoalTopic(GoalTopicDTO dto, Long studyId) {
        if (dto == null) return null;

        return new GoalTopic()
                .setStudyId(studyId)
                .setKey(dto.getKey())
                .setTitle(dto.getTitle())
                .setDescription(dto.getDescription())
                .setCreated(null)   // set by service layer
                .setModified(null);
    }

    public static List<GoalAdherenceCheck> toGoalAdherenceChecks(StudyGoalConfigDTO dto, Long studyId) {
        if (dto == null || dto.getSchedule() == null) return List.of();

        return dto.getSchedule().stream()
                .filter(Objects::nonNull)
                .map(s -> toGoalAdherenceCheck(s, studyId))
                .toList();
    }

    public static GoalAdherenceCheck toGoalAdherenceCheck(StudyGoalConfigScheduleInnerDTO dto, Long studyId) {
        if (dto == null) return null;

        return new GoalAdherenceCheck()
                .setStudyId(studyId)
                .setCheckId(dto.getKey() != null ? dto.getKey().ordinal() : null)
                .setTitle(mapScheduleEnumToTitle(dto.getKey()))
                .setTime(dto.getTime());   // LocalTime → LocalTime (direct)
    }

    public static GoalTemplate toGoalTemplate(GoalTemplateDTO dto, Long studyId) {
        return toGoalTemplate(dto, studyId, dto.getTemplateId());
    }

    public static GoalTemplate toGoalTemplate(GoalTemplateDTO dto, Long studyId, Integer goalTemplateId) {
        if (dto == null) return null;

        GoalTemplate template = new GoalTemplate()
                .setStudyId(studyId)
                .setTemplateId(goalTemplateId)
                .setStudyGroupId(dto.getStudyGroupId())
                .setTitle(dto.getTitle())
                .setParticipantTitle(dto.getParticipantTitle())
                .setParticipantInfo(dto.getParticipantInfo())
                .setType(dto.getType())
                .setKind(mapKindEnumToString(dto.getCategories() != null ? dto.getCategories().getKind() : null))
                .setCreated(dto.getCreated())
                .setModified(dto.getModified());

        // observationGroupIds
        if (dto.getObservationGroupIds() != null) {
            template.setObservationGroupIds(new HashSet<>(dto.getObservationGroupIds()));
        }

        // topicKeys
        if (dto.getCategories() != null && dto.getCategories().getTopics() != null) {
            template.setTopicKeys(new HashSet<>(dto.getCategories().getTopics()));
        }

        // Adherence Checks: Enum → Integer (ordinal)
        if (dto.getAdherenceChecks() != null) {
            Set<Integer> checkIds = dto.getAdherenceChecks().stream()
                    .map(GoalV1Transformer::mapAdherenceEnumToOrdinal)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            template.setAdherenceCheckIds(checkIds);
        }

        // properties
        if (dto.getProperties() != null && !dto.getProperties().isEmpty()) {
            GoalTemplateProperties props = new GoalTemplateProperties(dto.getProperties());
            template.setProperties(props);
        }

        return template;
    }
    // ========================== HELPER METHODS ==========================

    private static AdherenceCheckScheduleEnumDTO mapTitleToScheduleEnum(String title) {
        if (title == null) return null;
        String normalized = title.trim().toLowerCase();
        try {
            return AdherenceCheckScheduleEnumDTO.fromValue(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String mapScheduleEnumToTitle(AdherenceCheckScheduleEnumDTO enumValue) {
        return enumValue != null ? enumValue.getValue() : null;
    }
    private static String mapKindEnumToString(GoalTemplateCategoriesDTO.KindEnum kindEnum) {
        return kindEnum != null ? kindEnum.getValue() : null;
    }

    /**
     * Maps internal ordinal (stored in adherenceCheckIds) to DTO enum.
     */
    private static AdherenceCheckScheduleEnumDTO mapOrdinalToAdherenceEnum(Integer ordinal) {
        if (ordinal == null) return null;
        AdherenceCheckScheduleEnumDTO[] values = AdherenceCheckScheduleEnumDTO.values();
        if (ordinal >= 0 && ordinal < values.length) {
            return values[ordinal];
        }
        return null;
    }

    /**
     * Maps DTO enum to internal ordinal (for storage in adherenceCheckIds).
     */
    private static Integer mapAdherenceEnumToOrdinal(AdherenceCheckScheduleEnumDTO enumValue) {
        if (enumValue == null) return null;
        return enumValue.ordinal();
    }

    private static GoalTemplateCategoriesDTO.KindEnum mapKindToEnum(String kind) {
        if (kind == null) return null;
        try {
            return GoalTemplateCategoriesDTO.KindEnum.fromValue(kind.toLowerCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}