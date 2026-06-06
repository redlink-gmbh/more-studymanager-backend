/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.repository;

import io.redlink.more.studymanager.configuration.JPAConfiguration;
import io.redlink.more.studymanager.core.properties.GoalProperties;
import io.redlink.more.studymanager.model.*;
import io.redlink.more.studymanager.repository.goals.GoalRepository;
import io.redlink.more.studymanager.repository.goals.GoalTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@EnableAutoConfiguration
@ContextConfiguration(classes = {
        StudyComponentRepository.class,
        StudyRepository.class,
        StudyGroupRepository.class,
        ObservationRepository.class,
        InterventionRepository.class,
        ParticipantRepository.class,
        ObservationGroupRepository.class,
        GoalTemplateRepository.class,
        GoalRepository.class,
        JPAConfiguration.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test-containers-flyway")
class StudyComponentRepositoryTest {

    @Autowired
    private StudyComponentRepository studyComponentRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private ObservationRepository observationRepository;

    @Autowired
    private InterventionRepository interventionRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ObservationGroupRepository observationGroupRepository;

    @Autowired
    private GoalTemplateRepository goalTemplateRepository;

    @Autowired
    private GoalRepository goalRepository;

    @BeforeEach
    void deleteAll() {
        goalRepository.clear();
        goalTemplateRepository.clear();
        observationGroupRepository.clear();
        participantRepository.clear();
        interventionRepository.clear();
        observationRepository.clear();
        studyGroupRepository.clear();
    }

    @Test
    @DisplayName("Study components are retrieved correctly by study")
    void testGetComponents() {
        Long studyId = studyRepository.insert(new Study()
                        .setContact(new Contact().setPerson("test").setEmail("test")))
                .getStudyId();

        // 1. Study Group
        Integer studyGroupId = studyGroupRepository.insert(new StudyGroup()
                        .setStudyId(studyId)
                        .setTitle("Test Study Group")
                        .setPurpose("Test purpose"))
                .getStudyGroupId();

        // 2. Observation
        Integer observationId = observationRepository.insert(new Observation()
                        .setStudyId(studyId)
                        .setTitle("Test Observation")
                        .setType("questionnaire")
                        .setHidden(false)
                        .setPurpose("Test obs purpose"))
                .getObservationId();

        // 3. Intervention
        Integer interventionId = interventionRepository.insert(new Intervention()
                        .setStudyId(studyId)
                        .setTitle("Test Intervention")
                        .setPurpose("Test intv purpose"))
                .getInterventionId();

        // 4. Participant
        Integer participantId = participantRepository.insert(new Participant()
                        .setStudyId(studyId)
                        .setAlias("Test Participant")
                        .setRegistrationToken("TEST123"))
                .getParticipantId();

        // 5. Observation Group
        Integer observationGroupId = observationGroupRepository.insert(new ObservationGroup()
                        .setStudyId(studyId)
                        .setTitle("Test Observation Group")
                        .setPurpose("Test og purpose"))
                .getObservationGroupId();

        // 6. Goal Template
        Integer goalTemplateId = goalTemplateRepository.insert(new GoalTemplate()
                        .setStudyId(studyId)
                        .setTitle("Test Goal Template")
                        .setType("some-type"))
                .getTemplateId();

        // 7. Goal (linked to template)
        Integer goalId = goalRepository.insert(new Goal()
                        .setStudyId(studyId)
                        .setParticipantId(participantId)
                        .setTemplateId(goalTemplateId)
                        .setProperties(new GoalProperties(Map.of("progress", 50))))
                .getGoalId();

        // === Verify full retrieval ===
        Map<String, Map<Integer, StudyComponent>> allComponents = studyComponentRepository.getComponents(studyId);

        assertThat(allComponents).containsKeys(
                "studygroup", "observation", "intervention", "participant",
                "observationgroup", "goaltemplate", "goal"
        );

        assertThat(allComponents.get("studygroup").get(studyGroupId).title()).isEqualTo("Test Study Group");
        assertThat(allComponents.get("observation").get(observationId).title()).isEqualTo("Test Observation");
        assertThat(allComponents.get("intervention").get(interventionId).title()).isEqualTo("Test Intervention");
        assertThat(allComponents.get("participant").get(participantId).title()).isEqualTo("Test Participant");
        assertThat(allComponents.get("observationgroup").get(observationGroupId).title()).isEqualTo("Test Observation Group");
        assertThat(allComponents.get("goaltemplate").get(goalTemplateId).title()).isEqualTo("Test Goal Template");
        assertThat(allComponents.get("goal").get(goalId).title()).isEqualTo("Test Goal Template"); // title comes from template

        // Test filtered by type
        Map<String, Map<Integer, StudyComponent>> filtered = studyComponentRepository.getComponents(
                studyId, Set.of("participant", "goaltemplate", "goal")
        );

        assertThat(filtered).containsOnlyKeys("participant", "goaltemplate", "goal");
        assertThat(filtered.get("goal")).hasSize(1);
    }

    @Test
    @DisplayName("Single component retrieval works")
    void testGetComponent() {
        Long studyId = studyRepository.insert(new Study()
                        .setContact(new Contact().setPerson("test").setEmail("test")))
                .getStudyId();

        Integer participantId = participantRepository.insert(new Participant()
                        .setStudyId(studyId)
                        .setAlias("Single Test Participant")
                        .setRegistrationToken("TEST456"))
                .getParticipantId();

        StudyComponent component = studyComponentRepository.getComponent(studyId, "participant", participantId);

        assertThat(component).isNotNull();
        assertThat(component.studyId()).isEqualTo(studyId);
        assertThat(component.componentId()).isEqualTo(participantId);
        assertThat(component.type()).isEqualTo("participant");
        assertThat(component.title()).isEqualTo("Single Test Participant");

        // Non-existing returns null
        assertThat(studyComponentRepository.getComponent(studyId, "participant", 99999)).isNull();
    }

    @Test
    @DisplayName("Get components by single type")
    void testGetComponentsByType() {
        Long studyId = studyRepository.insert(new Study()
                        .setContact(new Contact().setPerson("test").setEmail("test")))
                .getStudyId();

        participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setAlias("P1")
                .setRegistrationToken("T1"));

        participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setAlias("P2")
                .setRegistrationToken("T2"));

        Map<Integer, StudyComponent> participants = studyComponentRepository.getComponents(studyId, "participant");

        assertThat(participants).hasSize(2);
        assertThat(participants.values()).allMatch(sc -> "participant".equals(sc.type()));
    }
}