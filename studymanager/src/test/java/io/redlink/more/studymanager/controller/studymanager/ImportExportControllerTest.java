/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.controller.studymanager;

import io.redlink.more.studymanager.api.v1.model.AdherenceCheckScheduleEnumDTO;
import io.redlink.more.studymanager.api.v1.model.GoalTemplateCategoriesDTO;
import io.redlink.more.studymanager.core.properties.ActionProperties;
import io.redlink.more.studymanager.core.properties.GoalTemplateProperties;
import io.redlink.more.studymanager.core.properties.ObservationProperties;
import io.redlink.more.studymanager.core.properties.TriggerProperties;
import io.redlink.more.studymanager.model.*;
import io.redlink.more.studymanager.model.scheduler.Event;
import io.redlink.more.studymanager.model.scheduler.RecurrenceRule;
import io.redlink.more.studymanager.repository.DownloadTokenRepository;
import io.redlink.more.studymanager.service.ImportExportService;
import io.redlink.more.studymanager.service.OAuth2AuthenticationService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest({ImportExportApiV1Controller.class})
@AutoConfigureMockMvc(addFilters = false)
class ImportExportControllerTest {

    @MockitoBean
    ImportExportService importExportService;

    @MockitoBean
    OAuth2AuthenticationService oAuth2AuthenticationService;

    @MockitoBean
    DownloadTokenRepository tokenRepository;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(oAuth2AuthenticationService.getCurrentUser()).thenReturn(
                new AuthenticatedUser(
                        UUID.randomUUID().toString(),
                        "Test User", "test@example.com", "Test Inc.",
                        EnumSet.allOf(PlatformRole.class)
                )
        );
    }

    @Test
    @DisplayName("Participants should be exported in csv format as a Resource")
    void testExportParticipants() throws Exception {

        String csv = "STUDYID;TITLE;PARTICIPANTID;ALIAS;REGISTRATIONTOKEN;REGISTRATIONURL\n1;Study;1;SomeAlias;SomeToken;http://examle.com/signup";

        when(importExportService.exportParticipants(any(Long.class), any()))
                .thenAnswer(invocationOnMock -> new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)));

        MvcResult result = mvc.perform(get("/api/v1/studies/1/export/participants")
                        .accept("text/csv")
                        .contentType("text/csv"))
                .andDo(print())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).isEqualTo(csv);
    }

    @Test
    @DisplayName("Test import/export of study configuration")
    void testImportExportStudy() throws Exception {
        Study study = new Study()
                .setStudyId(1L)
                .setTitle("testTitle")
                .setPurpose("testPurpose")
                .setParticipantInfo("testInfo")
                .setConsentInfo("testConsent")
                .setStudyState(Study.Status.DRAFT)
                .setPlannedStartDate(LocalDate.parse("2026-02-19"))
                .setPlannedEndDate(LocalDate.parse("2026-04-19"))
                .setCreated(Instant.now())
                .setModified(Instant.now());
        StudyGroup group = new StudyGroup()
                .setStudyId(study.getStudyId())
                .setStudyGroupId(1)
                .setTitle("test")
                .setPurpose("test")
                .setCreated(Instant.now())
                .setModified(Instant.now());
        ObservationGroup observationGroup1 = new ObservationGroup()
                .setStudyId(study.getStudyId())
                .setObservationGroupId(1)
                .setTitle("ObservationGroup 1")
                .setPurpose("test 1")
                .setCreated(Instant.now())
                .setModified(Instant.now());
        ObservationGroup observationGroup2 = new ObservationGroup()
                .setStudyId(study.getStudyId())
                .setObservationGroupId(2)
                .setTitle("ObservationGroup 2")
                .setPurpose("test 2")
                .setCreated(Instant.now())
                .setModified(Instant.now());
        Observation observation = new Observation()
                .setStudyId(study.getStudyId())
                .setTitle("testTitle")
                .setPurpose("testPurpose")
                .setParticipantInfo("testInfo")
                .setType("testType")
                .setStudyGroupId(group.getStudyGroupId())
                .setProperties(new ObservationProperties())
                .setCreated(Instant.now())
                .setSchedule(new Event())
                .setObservationGroupIds(Set.of(1))
                .setCreated(Instant.now())
                .setModified(Instant.now());
        Intervention intervention = new Intervention()
                .setInterventionId(1)
                .setStudyId(study.getStudyId())
                .setTitle("some title")
                .setStudyGroupId(group.getStudyGroupId())
                .setObservationGroupIds(Set.of(2))
                .setSchedule(new Event().setDateEnd(Instant.now()).setDateEnd(Instant.now().plusSeconds(60)))
                .setSchedule(new Event()
                        .setDateStart(Instant.parse("2025-11-12T10:00:00Z"))
                        .setDateEnd(Instant.parse("2025-11-12T12:00:00Z"))
                        .setRRule(new RecurrenceRule().setFreq("DAILY").setCount(7)))
                .setCreated(Instant.now())
                .setModified(Instant.now());

        Trigger trigger = new Trigger()
                .setProperties(new TriggerProperties(Map.of("property", "new value")))
                .setCreated(Instant.now())
                .setModified(Instant.now());

        Action action = new Action()
                .setActionId(1)
                .setProperties(new ActionProperties(Map.of("property", "new value")))
                .setCreated(Instant.now())
                .setModified(Instant.now());

        StudyImportExport.StudyGoalConfigData goalConfig = new StudyImportExport.StudyGoalConfigData(study.getStudyId());
        goalConfig.setAchievability("Ist das Ziel erreichbar?");
        goalConfig.setCommitment("Bist Du motiviert das Ziel zu erreichen?");
        goalConfig.setUnderstandability("Ist das Ziel verständlich?");
        goalConfig.setAdherenceChecks(List.of(
                new GoalAdherenceCheck()
                        .setStudyId(study.getStudyId())
                        .setCheckId(AdherenceCheckScheduleEnumDTO.NOON.ordinal())
                        .setTitle(AdherenceCheckScheduleEnumDTO.NOON.getValue())
                        .setTime(LocalTime.parse("12:00:00")),
                new GoalAdherenceCheck()
                        .setStudyId(study.getStudyId())
                        .setCheckId(AdherenceCheckScheduleEnumDTO.EVENING.ordinal())
                        .setTitle(AdherenceCheckScheduleEnumDTO.EVENING.getValue())
                        .setTime(LocalTime.parse("20:00:00"))
        ));
        goalConfig.setTopics(List.of(
                new GoalTopic()
                        .setStudyId(study.getStudyId())
                        .setKey("drink")
                        .setTitle("Trinken")
                        .setDescription("Trinken Beschreibung"),
                new GoalTopic()
                        .setStudyId(study.getStudyId())
                        .setKey("eat")
                        .setTitle("Essen")
                        .setDescription("Essen Beschreibung")
        ));
        GoalTemplate goalTemplate = new GoalTemplate()
                .setStudyId(study.getStudyId())
                .setTemplateId(1)
                .setType("eatAmountOf")
                .setKind(GoalTemplateCategoriesDTO.KindEnum.BEHAVIORAL.getValue())
                .setAdherenceCheckIds(Set.of(
                        AdherenceCheckScheduleEnumDTO.NOON.ordinal(),
                        AdherenceCheckScheduleEnumDTO.EVENING.ordinal()))
                .setTopicKeys(Set.of("eat"))
                .setParticipantTitle("Portionen Obst Essen")
                .setParticipantInfo("Jeden Tag Obst Essen")
                .setStudyGroupId(group.getStudyGroupId())
                .setObservationGroupIds(Set.of(observationGroup1.getObservationGroupId()))
                .setTitle("Obst Essen")
                .setProperties(new GoalTemplateProperties(Map.of("property", "new value")));

        Milestone milestone = new Milestone()
                .setStudyId(study.getStudyId())
                .setMilestoneId(1)
                .setName("Baseline")
                .setOrderIndex(0)
                .setCreated(Instant.now());

        StudyImportExport.ParticipantInfo participantInfo = new StudyImportExport.ParticipantInfo(
                group.getStudyGroupId(),
                Set.of(),
                List.of(new ParticipantMilestoneInfo(milestone.getMilestoneId(), Instant.parse("2026-03-01T09:00:00Z"))));

        StudyImportExport studyImportExport = new StudyImportExport()
                .setStudy(study)
                .setStudyGroups(List.of(group))
                .setObservationGroups(List.of(observationGroup1, observationGroup2))
                .setObservations(List.of(observation))
                .setInterventions(List.of(intervention))
                .setMilestones(List.of(milestone))
                .setTriggers(Map.of(intervention.getInterventionId(), trigger))
                .setActions(Map.of(intervention.getInterventionId(), List.of(action)))
                .setParticipants(List.of(participantInfo))
                .setIntegrations(new ArrayList<>())
                .setStudyGoalConfig(goalConfig)
                .setGoalTemplates(List.of(goalTemplate));

        when(importExportService.exportStudy(anyLong(), any()))
                .thenAnswer(invocationOnMock -> studyImportExport);
        when(importExportService.importStudy(any(StudyImportExport.class), any()))
                .thenAnswer(invocationOnMock -> {
                    var data = invocationOnMock.getArgument(0, StudyImportExport.class);
                    assertThat(data.getStudyGoalConfig()).isNotNull();
                    assertThat(data.getStudyGoalConfig().getAchievability()).isEqualTo("Ist das Ziel erreichbar?");
                    assertThat(data.getStudyGoalConfig().getCommitment()).isEqualTo("Bist Du motiviert das Ziel zu erreichen?");
                    assertThat(data.getStudyGoalConfig().getUnderstandability()).isEqualTo("Ist das Ziel verständlich?");
                    assertThat(data.getStudyGoalConfig().getTopics())
                            .hasSize(2)
                            .extracting("key", "title", "description")
                            .containsExactlyInAnyOrder(
                                    tuple(
                                            "eat",
                                            "Essen",
                                            "Essen Beschreibung"
                                    ),
                                    tuple(
                                            "drink",
                                            "Trinken",
                                            "Trinken Beschreibung"
                                    )
                            );
                    assertThat(data.getStudyGoalConfig().getAdherenceChecks())
                            .hasSize(2)
                            .extracting("checkId", "title", "time")
                            .containsExactlyInAnyOrder(
                                    tuple(
                                            AdherenceCheckScheduleEnumDTO.NOON.ordinal(),
                                            AdherenceCheckScheduleEnumDTO.NOON.getValue(),
                                            LocalTime.parse("12:00:00")
                                    ),
                                    tuple(
                                            AdherenceCheckScheduleEnumDTO.EVENING.ordinal(),
                                            AdherenceCheckScheduleEnumDTO.EVENING.getValue(),
                                            LocalTime.parse("20:00:00")
                                    )
                            );
                    assertThat(data.getGoalTemplates()).hasSize(1);
                    assertThat(data.getGoalTemplates().get(0).getStudyId()).isEqualTo(1);
                    assertThat(data.getGoalTemplates().get(0).getTemplateId()).isEqualTo(1);
                    assertThat(data.getGoalTemplates().get(0).getType()).isEqualTo("eatAmountOf");
                    assertThat(data.getGoalTemplates().get(0).getKind()).isEqualTo(GoalTemplateCategoriesDTO.KindEnum.BEHAVIORAL.getValue());
                    assertThat(data.getGoalTemplates().get(0).getAdherenceCheckIds()).hasSize(2);
                    assertThat(data.getGoalTemplates().get(0).getAdherenceCheckIds()).containsExactlyInAnyOrder(
                            AdherenceCheckScheduleEnumDTO.EVENING.ordinal(),
                            AdherenceCheckScheduleEnumDTO.NOON.ordinal());
                    assertThat(data.getGoalTemplates().get(0).getTitle()).isEqualTo("Obst Essen");
                    assertThat(data.getGoalTemplates().get(0).getParticipantTitle()).isEqualTo("Portionen Obst Essen");
                    assertThat(data.getGoalTemplates().get(0).getParticipantInfo()).isEqualTo("Jeden Tag Obst Essen");
                    assertThat(data.getGoalTemplates().get(0).getProperties().get("property")).isEqualTo("new value");
                    assertThat(data.getGoalTemplates().get(0).getStudyGroupId()).isEqualTo(group.getStudyGroupId());
                    assertThat(data.getGoalTemplates().get(0).getObservationGroupIds()).containsExactlyInAnyOrder(observationGroup1.getObservationGroupId());

                    assertThat(data.getMilestones()).hasSize(1);
                    assertThat(data.getMilestones().get(0).getMilestoneId()).isEqualTo(milestone.getMilestoneId());
                    assertThat(data.getMilestones().get(0).getName()).isEqualTo(milestone.getName());
                    assertThat(data.getMilestones().get(0).getOrderIndex()).isEqualTo(milestone.getOrderIndex());

                    assertThat(data.getParticipants()).hasSize(1);
                    assertThat(data.getParticipants().get(0).groupId()).isEqualTo(group.getStudyGroupId());
                    assertThat(data.getParticipants().get(0).milestones()).containsExactly(
                            new ParticipantMilestoneInfo(milestone.getMilestoneId(), Instant.parse("2026-03-01T09:00:00Z")));

                    return data
                            .getStudy()
                            .setStudyId(2L)
                            .setStudyState(Study.Status.DRAFT)
                            .setCreated(Instant.ofEpochMilli(0))
                            .setModified(Instant.ofEpochMilli(0));
                });


        MvcResult resultExport = mvc.perform(get("/api/v1/studies/1/export/study")
                        .contentType("application/json"))
                .andDo(print())
                .andExpect(jsonPath("$.study").isMap())
                .andExpect(jsonPath("$.study.studyId").value(1))
                .andExpect(jsonPath("$.study.title").value("testTitle"))
                .andExpect(jsonPath("$.study.purpose").value("testPurpose"))
                .andExpect(jsonPath("$.study.participantInfo").value("testInfo"))
                .andExpect(jsonPath("$.study.consentInfo").value("testConsent"))
                .andExpect(jsonPath("$.study.duration").isEmpty())
                .andExpect(jsonPath("$.study.finishText").isEmpty())
                .andExpect(jsonPath("$.study.status").value("draft"))
                .andExpect(jsonPath("$.study.start").isEmpty())
                .andExpect(jsonPath("$.study.end").isEmpty())
                .andExpect(jsonPath("$.study.plannedStart").value("2026-02-19"))
                .andExpect(jsonPath("$.study.plannedEnd").value("2026-04-19"))
                .andExpect(jsonPath("$.study.created").exists())
                .andExpect(jsonPath("$.study.modified").exists())
                .andExpect(jsonPath("$.study.userRoles").isEmpty())
                .andExpect(jsonPath("$.study.contact").isMap())
                .andExpect(jsonPath("$.study.contact.institute").isEmpty())
                .andExpect(jsonPath("$.study.contact.person").isEmpty())
                .andExpect(jsonPath("$.study.contact.email").isEmpty())
                .andExpect(jsonPath("$.study.contact.phoneNumber").isEmpty())
                .andExpect(jsonPath("$.studyGroups").isArray())
                .andExpect(jsonPath("$.studyGroups.length()").value(1))
                .andExpect(jsonPath("$.studyGroups[0].studyId").value(1))
                .andExpect(jsonPath("$.studyGroups[0].studyGroupId").value(1))
                .andExpect(jsonPath("$.studyGroups[0].title").value("test"))
                .andExpect(jsonPath("$.studyGroups[0].purpose").value("test"))
                .andExpect(jsonPath("$.studyGroups[0].duration").isEmpty())
                .andExpect(jsonPath("$.studyGroups[0].numberOfParticipants").isEmpty())
                .andExpect(jsonPath("$.studyGroups[0].created").exists())
                .andExpect(jsonPath("$.studyGroups[0].modified").exists())
                .andExpect(jsonPath("$.observationGroups").isArray())
                .andExpect(jsonPath("$.observationGroups.length()").value(2))
                .andExpect(jsonPath("$.observationGroups[0].studyId").value(1))
                .andExpect(jsonPath("$.observationGroups[0].observationGroupId").value(1))
                .andExpect(jsonPath("$.observationGroups[0].title").value("ObservationGroup 1"))
                .andExpect(jsonPath("$.observationGroups[0].purpose").value("test 1"))
                .andExpect(jsonPath("$.observationGroups[0].numberOfParticipants").isEmpty())
                .andExpect(jsonPath("$.observationGroups[0].numberOfObservations").isEmpty())
                .andExpect(jsonPath("$.observationGroups[0].numberOfInterventions").isEmpty())
                .andExpect(jsonPath("$.observationGroups[0].created").exists())
                .andExpect(jsonPath("$.observationGroups[0].modified").exists())
                .andExpect(jsonPath("$.observationGroups[1].studyId").value(1))
                .andExpect(jsonPath("$.observationGroups[1].observationGroupId").value(2))
                .andExpect(jsonPath("$.observationGroups[1].title").value("ObservationGroup 2"))
                .andExpect(jsonPath("$.observationGroups[1].purpose").value("test 2"))
                .andExpect(jsonPath("$.observationGroups[1].numberOfParticipants").isEmpty())
                .andExpect(jsonPath("$.observationGroups[1].numberOfObservations").isEmpty())
                .andExpect(jsonPath("$.observationGroups[1].numberOfInterventions").isEmpty())
                .andExpect(jsonPath("$.observationGroups[1].created").exists())
                .andExpect(jsonPath("$.observationGroups[1].modified").exists())
                .andExpect(jsonPath("$.observations").isArray())
                .andExpect(jsonPath("$.observations.length()").value(1))
                .andExpect(jsonPath("$.observations[0].studyId").value(1))
                .andExpect(jsonPath("$.observations[0].observationId").isEmpty())
                .andExpect(jsonPath("$.observations[0].studyGroupId").value(1))
                .andExpect(jsonPath("$.observations[0].title").value("testTitle"))
                .andExpect(jsonPath("$.observations[0].purpose").value("testPurpose"))
                .andExpect(jsonPath("$.observations[0].participantInfo").value("testInfo"))
                .andExpect(jsonPath("$.observations[0].type").value("testType"))
                .andExpect(jsonPath("$.observations[0].properties").isEmpty())
                .andExpect(jsonPath("$.observations[0].schedule").isMap())
                .andExpect(jsonPath("$.observations[0].schedule.type").value("Event"))
                .andExpect(jsonPath("$.observations[0].schedule.dtstart").isEmpty())
                .andExpect(jsonPath("$.observations[0].schedule.dtend").isEmpty())
                .andExpect(jsonPath("$.observations[0].schedule.rrule").isEmpty())
                .andExpect(jsonPath("$.observations[0].schedule.random").isEmpty())
                .andExpect(jsonPath("$.observations[0].created").exists())
                .andExpect(jsonPath("$.observations[0].modified").exists())
                .andExpect(jsonPath("$.observations[0].hidden").isEmpty())
                .andExpect(jsonPath("$.observations[0].noSchedule").value(false))
                .andExpect(jsonPath("$.observations[0].reminder").value(false))
                .andExpect(jsonPath("$.observations[0].observationGroupIds").isArray())
                .andExpect(jsonPath("$.observations[0].observationGroupIds.length()").value(1))
                .andExpect(jsonPath("$.observations[0].observationGroupIds[0]").value(1))
                .andExpect(jsonPath("$.interventions").isArray())
                .andExpect(jsonPath("$.interventions.length()").value(1))
                .andExpect(jsonPath("$.interventions[0].studyId").value(1))
                .andExpect(jsonPath("$.interventions[0].interventionId").value(1))
                .andExpect(jsonPath("$.interventions[0].studyGroupId").value(1))
                .andExpect(jsonPath("$.interventions[0].title").value("some title"))
                .andExpect(jsonPath("$.interventions[0].purpose").isEmpty())
                .andExpect(jsonPath("$.interventions[0].schedule").isMap())
                .andExpect(jsonPath("$.interventions[0].schedule.type").value("Event"))
                .andExpect(jsonPath("$.interventions[0].schedule.dtstart").value("2025-11-12T10:00:00Z"))
                .andExpect(jsonPath("$.interventions[0].schedule.dtend").value("2025-11-12T12:00:00Z"))
                .andExpect(jsonPath("$.interventions[0].schedule.rrule").isMap())
                .andExpect(jsonPath("$.interventions[0].schedule.rrule.freq").value("DAILY"))
                .andExpect(jsonPath("$.interventions[0].schedule.rrule.until").isEmpty())
                .andExpect(jsonPath("$.interventions[0].schedule.rrule.count").value(7))
                .andExpect(jsonPath("$.interventions[0].schedule.rrule.interval").isEmpty())
                .andExpect(jsonPath("$.interventions[0].schedule.rrule.byday").isEmpty())
                .andExpect(jsonPath("$.interventions[0].schedule.rrule.bymonth").isEmpty())
                .andExpect(jsonPath("$.interventions[0].schedule.rrule.bymonthday").isEmpty())
                .andExpect(jsonPath("$.interventions[0].schedule.rrule.bysetpos").isEmpty())
                .andExpect(jsonPath("$.interventions[0].schedule.random").isEmpty())
                .andExpect(jsonPath("$.interventions[0].trigger").isMap())
                .andExpect(jsonPath("$.interventions[0].trigger.type").isEmpty())
                .andExpect(jsonPath("$.interventions[0].trigger.properties").isMap())
                .andExpect(jsonPath("$.interventions[0].trigger.properties.property").value("new value"))
                .andExpect(jsonPath("$.interventions[0].trigger.created").exists())
                .andExpect(jsonPath("$.interventions[0].trigger.modified").exists())
                .andExpect(jsonPath("$.interventions[0].actions").isArray())
                .andExpect(jsonPath("$.interventions[0].actions.length()").value(1))
                .andExpect(jsonPath("$.interventions[0].actions[0].actionId").value(1))
                .andExpect(jsonPath("$.interventions[0].actions[0].type").isEmpty())
                .andExpect(jsonPath("$.interventions[0].actions[0].properties").isMap())
                .andExpect(jsonPath("$.interventions[0].actions[0].properties.property").value("new value"))
                .andExpect(jsonPath("$.interventions[0].actions[0].created").exists())
                .andExpect(jsonPath("$.interventions[0].actions[0].modified").exists())
                .andExpect(jsonPath("$.interventions[0].observationGroupIds").isArray())
                .andExpect(jsonPath("$.interventions[0].observationGroupIds.length()").value(1))
                .andExpect(jsonPath("$.interventions[0].observationGroupIds[0]").value(2))
                .andExpect(jsonPath("$.interventions[0].created").exists())
                .andExpect(jsonPath("$.interventions[0].modified").exists())
                .andExpect(jsonPath("$.milestones").isArray())
                .andExpect(jsonPath("$.milestones.length()").value(1))
                .andExpect(jsonPath("$.milestones[0].milestoneId").value(1))
                .andExpect(jsonPath("$.milestones[0].studyId").value(1))
                .andExpect(jsonPath("$.milestones[0].name").value("Baseline"))
                .andExpect(jsonPath("$.milestones[0].orderIndex").value(0))
                .andExpect(jsonPath("$.participants").isArray())
                .andExpect(jsonPath("$.participants.length()").value(1))
                .andExpect(jsonPath("$.participants[0].studyGroup").value(group.getStudyGroupId()))
                .andExpect(jsonPath("$.participants[0].milestones").isArray())
                .andExpect(jsonPath("$.participants[0].milestones.length()").value(1))
                .andExpect(jsonPath("$.participants[0].milestones[0].milestoneId").value(1))
                .andExpect(jsonPath("$.participants[0].milestones[0].dateTime").value("2026-03-01T09:00:00Z"))
                .andExpect(jsonPath("$.integrations").isArray())
                .andExpect(jsonPath("$.integrations.length()").value(0))
                .andExpect(jsonPath("$.goalConfiguration").exists())
                .andExpect(jsonPath("$.goalConfiguration.consent").exists())
                .andExpect(jsonPath("$.goalConfiguration.consent.achievability").value("Ist das Ziel erreichbar?"))
                .andExpect(jsonPath("$.goalConfiguration.consent.commitment").value("Bist Du motiviert das Ziel zu erreichen?"))
                .andExpect(jsonPath("$.goalConfiguration.consent.understandability").value("Ist das Ziel verständlich?"))
                .andExpect(jsonPath("$.goalConfiguration.adherenceChecks").isArray())
                .andExpect(jsonPath("$.goalConfiguration.adherenceChecks[0].check").value(AdherenceCheckScheduleEnumDTO.NOON.getValue()))
                .andExpect(jsonPath("$.goalConfiguration.adherenceChecks[0].time").value("12:00:00"))
                .andExpect(jsonPath("$.goalConfiguration.adherenceChecks[1].check").value(AdherenceCheckScheduleEnumDTO.EVENING.getValue()))
                .andExpect(jsonPath("$.goalConfiguration.adherenceChecks[1].time").value("20:00:00"))
                .andExpect(jsonPath("$.goalConfiguration.topics").isArray())
                .andExpect(jsonPath("$.goalConfiguration.topics[0].key").value("drink"))
                .andExpect(jsonPath("$.goalConfiguration.topics[0].title").value("Trinken"))
                .andExpect(jsonPath("$.goalConfiguration.topics[0].description").value("Trinken Beschreibung"))
                .andExpect(jsonPath("$.goalConfiguration.topics[1].key").value("eat"))
                .andExpect(jsonPath("$.goalConfiguration.topics[1].title").value("Essen"))
                .andExpect(jsonPath("$.goalConfiguration.topics[1].description").value("Essen Beschreibung"))
                .andExpect(jsonPath("$.goalTemplates").isArray())
                .andExpect(jsonPath("$.goalTemplates[0].studyId").value(1))
                .andExpect(jsonPath("$.goalTemplates[0].templateId").value(1))
                .andExpect(jsonPath("$.goalTemplates[0].studyGroupId").value(1))
                .andExpect(jsonPath("$.goalTemplates[0].observationGroupIds.length()").value(1))
                .andExpect(jsonPath("$.goalTemplates[0].observationGroupIds[0]").value(1))
                .andExpect(jsonPath("$.goalTemplates[0].title").value("Obst Essen"))
                .andExpect(jsonPath("$.goalTemplates[0].participantTitle").value("Portionen Obst Essen"))
                .andExpect(jsonPath("$.goalTemplates[0].participantInfo").value("Jeden Tag Obst Essen"))
                .andExpect(jsonPath("$.goalTemplates[0].type").value("eatAmountOf"))
                .andExpect(jsonPath("$.goalTemplates[0].categories").exists())
                .andExpect(jsonPath("$.goalTemplates[0].categories.kind").value(GoalTemplateCategoriesDTO.KindEnum.BEHAVIORAL.getValue()))
                .andExpect(jsonPath("$.goalTemplates[0].categories.topics").isArray())
                .andExpect(jsonPath("$.goalTemplates[0].categories.topics.length()").value(1))
                .andExpect(jsonPath("$.goalTemplates[0].categories.topics[0]").value("eat"))
                .andExpect(jsonPath("$.goalTemplates[0].adherenceChecks").isArray())
                .andExpect(jsonPath("$.goalTemplates[0].adherenceChecks.length()").value(2))
                .andExpect(jsonPath("$.goalTemplates[0].adherenceChecks").value(Matchers.containsInAnyOrder(
                        AdherenceCheckScheduleEnumDTO.EVENING.getValue(),
                        AdherenceCheckScheduleEnumDTO.NOON.getValue())))
                .andExpect(jsonPath("$.goalTemplates[0].properties").exists())
                .andExpect(jsonPath("$.goalTemplates[0].properties.property").value("new value"))
                .andReturn();


        mvc.perform(
                        multipart("/api/v1/studies/import/study")
                                .file("file", resultExport.getResponse().getContentAsByteArray()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(study.getTitle()))
                .andExpect(jsonPath("$.studyId").value(2L))
                .andExpect(jsonPath("$.purpose").value(study.getPurpose()))
                .andExpect(jsonPath("$.consentInfo").value(study.getConsentInfo()))
                .andExpect(jsonPath("$.participantInfo").value(study.getParticipantInfo()))
                .andExpect(jsonPath("$.plannedStart").exists())
                .andExpect(jsonPath("$.plannedEnd").exists())
                .andExpect(jsonPath("$.modified").exists())
                .andExpect(jsonPath("$.created").exists());
    }
}
