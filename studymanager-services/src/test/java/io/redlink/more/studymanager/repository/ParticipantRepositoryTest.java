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
import io.redlink.more.studymanager.model.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.TaskExecutionOutcome;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@EnableAutoConfiguration
@ContextConfiguration(classes = {
        ParticipantRepository.class, StudyRepository.class, StudyGroupRepository.class, ObservationGroupRepository.class,
        JPAConfiguration.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test-containers-flyway")
class ParticipantRepositoryTest {

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private ObservationGroupRepository observationGroupRepository;

    @BeforeEach
    void deleteAll() {
        participantRepository.clear();
    }

    @Test
    @DisplayName("Participant is inserted and returned")
    void testInsert() throws InterruptedException {
        Long studyId = studyRepository.insert(new Study().setContact(new Contact().setPerson("test").setEmail("test"))).getStudyId();
        Integer studyGroupId = studyGroupRepository.insert(new StudyGroup()
                .setStudyId(studyId)).getStudyGroupId();

        Integer observationGroupId1 = observationGroupRepository.insert(new ObservationGroup().setStudyId(studyId).setTitle("Observation Group 1").setPurpose("test")).getObservationGroupId();
        Integer observationGroupId2 = observationGroupRepository.insert(new ObservationGroup().setStudyId(studyId).setTitle("Observation Group 2").setPurpose("test")).getObservationGroupId();
        Integer observationGroupId3 = observationGroupRepository.insert(new ObservationGroup().setStudyId(studyId).setTitle("Observation Group 3").setPurpose("test")).getObservationGroupId();

        Participant participant = new Participant()
                .setAlias("participant x")
                .setStudyGroupId(studyGroupId)
                .setStudyId(studyId)
                .setRegistrationToken("TEST123")
                .setObservationGroupIds(Set.of(observationGroupId1, observationGroupId2));

        Participant participantResponse = participantRepository.insert(participant);

        assertThat(participantResponse.getAlias()).isEqualTo(participant.getAlias());
        assertThat(participantResponse.getStatus()).isEqualTo(Participant.Status.NEW);
        assertThat(participantResponse.getParticipantId()).isNotNull();
        assertThat(participantResponse.getObservationGroupIds()).containsExactlyInAnyOrder(observationGroupId1, observationGroupId2);

        Participant update = participantResponse
                .setAlias("new participant x")
                .setObservationGroupIds(Set.of(observationGroupId1, observationGroupId3)); //replace observationGroup1 with observationGroup3

        TimeUnit.MICROSECONDS.sleep(1); //to ensure a different modified time

        Participant updated = participantRepository.update(update);

        assertThat(update.getStudyId()).isEqualTo(participantResponse.getStudyId());
        assertThat(update.getAlias()).isEqualTo(updated.getAlias());
        assertThat(update.getObservationGroupIds()).containsExactlyInAnyOrder(observationGroupId1, observationGroupId3);
        assertThat(updated.getCreated()).isEqualTo(participantResponse.getCreated());
        assertThat(updated.getModified()).isAfter(participantResponse.getModified());

        Participant queried = participantRepository.getByIds(participantResponse.getStudyId(), participantResponse.getParticipantId());

        assertThat(queried.getAlias()).isEqualTo(updated.getAlias());
        assertThat(queried.getStudyId()).isEqualTo(updated.getStudyId());
        assertThat(queried.getCreated()).isEqualTo(updated.getCreated());
        assertThat(queried.getStatus()).isEqualTo(updated.getStatus());
        assertThat(queried.getObservationGroupIds()).containsExactlyInAnyOrder(observationGroupId1, observationGroupId3);

    }

    @Test
    @DisplayName("Participants are deleted and listed correctly")
    void testListAndDelete() {
        Long studyId = studyRepository.insert(new Study().setContact(new Contact().setPerson("test").setEmail("test"))).getStudyId();
        Integer studyGroupId = studyGroupRepository.insert(new StudyGroup().setStudyId(studyId).setTitle("Study Group 1")).getStudyGroupId();
        Integer observationGroupId1 = observationGroupRepository.insert(new ObservationGroup().setStudyId(studyId).setTitle("Observation Group 1").setPurpose("test")).getObservationGroupId();
        Integer observationGroupId2 = observationGroupRepository.insert(new ObservationGroup().setStudyId(studyId).setTitle("Observation Group 2").setPurpose("test")).getObservationGroupId();
        Integer observationGroupId3 = observationGroupRepository.insert(new ObservationGroup().setStudyId(studyId).setTitle("Observation Group 3").setPurpose("test")).getObservationGroupId();

        Participant p1 = participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setObservationGroupIds(Set.of(observationGroupId1, observationGroupId2))
                .setRegistrationToken("TEST123"));
        Participant p2 = participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setStudyGroupId(studyGroupId)
                .setObservationGroupIds(Set.of(observationGroupId1, observationGroupId3))
                .setRegistrationToken("TEST456"));
        Participant p3 = participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setStudyGroupId(studyGroupId)
                .setObservationGroupIds(Set.of(observationGroupId3))
                .setRegistrationToken("TEST789"));

        assertThat(participantRepository.listParticipants(studyId))
                .hasSize(3);

        Participant p4 = participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setRegistrationToken("TEST012"));

        Participant p5 = participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setStudyGroupId(studyGroupId)
                .setRegistrationToken("TEST345"));

        Participant p6 = participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setStudyGroupId(studyGroupId)
                .setRegistrationToken("TEST678")
                .setObservationGroupIds(Set.of(observationGroupId1)));

        var participantsSg1Og23 = participantRepository.listParticipants(studyId, studyGroupId, Set.of(observationGroupId2, observationGroupId3));
        assertThat(participantsSg1Og23).hasSize(3);
        assertThat(participantsSg1Og23).extracting(Participant::getParticipantId).containsExactlyInAnyOrder(p2.getParticipantId(), p3.getParticipantId(), p5.getParticipantId());
        var participantsOg23 = participantRepository.listParticipants(studyId, null, Set.of(observationGroupId2, observationGroupId3));
        assertThat(participantsOg23).hasSize(5); //any study group
        assertThat(participantsOg23).extracting(Participant::getParticipantId).containsExactlyInAnyOrder(p1.getParticipantId(), p2.getParticipantId(), p3.getParticipantId(), p4.getParticipantId(), p5.getParticipantId());
        //parsing NULL as observationGroups means no filter
        var participantsNullParams = participantRepository.listParticipants(studyId, null, null);
        assertThat(participantsNullParams).hasSize(6); //returns all

        var participantsNullStudyGroupNoObservationGroup = participantRepository.listParticipants(studyId, null, Set.of());
        assertThat(participantsNullStudyGroupNoObservationGroup).hasSize(2); //returns all
        assertThat(participantsNullStudyGroupNoObservationGroup).extracting(Participant::getParticipantId).containsExactlyInAnyOrder(p4.getParticipantId(), p5.getParticipantId());

        var participantsSg1 = participantRepository.listParticipants(studyId, studyGroupId, null);
        assertThat(participantsSg1).hasSize(4); //returns all
        assertThat(participantsSg1).extracting(Participant::getParticipantId).containsExactlyInAnyOrder(p2.getParticipantId(), p3.getParticipantId(), p5.getParticipantId(),p6.getParticipantId());

        var participantsSg1NoObservationGroup = participantRepository.listParticipants(studyId, studyGroupId, Set.of());
        assertThat(participantsSg1NoObservationGroup).hasSize(1); //returns all
        assertThat(participantsSg1NoObservationGroup).extracting(Participant::getParticipantId).containsExactlyInAnyOrder(p5.getParticipantId());


        participantRepository.deleteParticipant(studyId, p1.getParticipantId());
        assertThat(participantRepository.listParticipants(studyId))
                .hasSize(5);
        participantRepository.deleteParticipant(studyId, p2.getParticipantId());
        assertThat(participantRepository.listParticipants(studyId))
                .hasSize(4);
        participantRepository.deleteParticipant(studyId, p2.getParticipantId());
        assertThat(participantRepository.listParticipants(studyId))
                .hasSize(4);
        participantRepository.deleteParticipant(studyId, p3.getParticipantId());
        participantRepository.deleteParticipant(studyId, p4.getParticipantId());
        participantRepository.deleteParticipant(studyId, p5.getParticipantId());
        participantRepository.deleteParticipant(studyId, p6.getParticipantId());
        assertThat(participantRepository.listParticipants(studyId))
                .isEmpty();
    }

    private Participant createParticipant(Long studyId, Participant.Status status) {
        final Participant p = createParticipant(studyId);
        if (status != null) {
            return participantRepository.setStatusByIds(p.getStudyId(), p.getParticipantId(), status)
                    .orElseThrow();
        }
        return p;

    }

    private Participant createParticipant(Long studyId) {
        return participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setRegistrationToken(RandomStringUtils.randomAlphanumeric(12))
        );
    }

    @Test
    @DisplayName("Participant states are set correctly")
    void testSetState() {
        Long studyId = studyRepository.insert(new Study().setContact(new Contact().setPerson("test").setEmail("test"))).getStudyId();

        Integer observationGroup1 = observationGroupRepository.insert(new ObservationGroup().setStudyId(studyId).setTitle("Observation Group 1").setPurpose("Purpose 1")).getObservationGroupId();
        Integer observationGroup2 = observationGroupRepository.insert(new ObservationGroup().setStudyId(studyId).setTitle("Observation Group 2").setPurpose("Purpose 2")).getObservationGroupId();

        Participant participant = participantRepository.insert(new Participant().setStudyId(studyId).setRegistrationToken("TEST123").setObservationGroupIds(Set.of(observationGroup1, observationGroup2)));
        assertThat(participant.getStatus()).isEqualTo(Participant.Status.NEW);

        participant = participantRepository.getByIds(studyId, participant.getParticipantId());
        assertThat(participant.getStatus()).isEqualTo(Participant.Status.NEW);

        participant = participantRepository.setStatusByIds(studyId, participant.getParticipantId(), Participant.Status.ACTIVE).get();
        assertThat(participant.getStatus()).isEqualTo(Participant.Status.ACTIVE);
        //Assert that the SQL query for the status update correctly retrieves the observation groups for the participant
        assertThat(participant.getObservationGroupIds()).containsExactlyInAnyOrder(observationGroup1, observationGroup2);

        //make an additional retrieval just to be sure
        participant = participantRepository.getByIds(studyId, participant.getParticipantId());
        assertThat(participant.getStatus()).isEqualTo(Participant.Status.ACTIVE);
        //Assert that the SQL query for the status update correctly retrieves the observation groups for the participant
        assertThat(participant.getObservationGroupIds()).containsExactlyInAnyOrder(observationGroup1, observationGroup2);
    }

    @Test
    @DisplayName("Participants study group must be undefined")
    void testUndefinedStudyGroup() {
        Long studyId = studyRepository.insert(new Study().setContact(new Contact().setPerson("test").setEmail("test"))).getStudyId();
        Participant participant = participantRepository
                .insert(new Participant().setStudyId(studyId).setRegistrationToken("abc"));
        assertThat(participant.getStudyGroupId()).isNull();
    }

    @Test
    @DisplayName("Participants for closing")
    void testClosing() {
        Study study = studyRepository.insert(new Study().setContact(new Contact().setPerson("test").setEmail("test")));
        studyRepository.setStateById(study.getStudyId(), Study.Status.ACTIVE);
        Participant participant = participantRepository
                .insert(new Participant().setStudyId(study.getStudyId()).setRegistrationToken("abc").setStart(Instant.now()));
        participantRepository.setStatusByIds(study.getStudyId(), participant.getParticipantId(), Participant.Status.ACTIVE);

        var participants = participantRepository.listParticipantsForClosing();
    }


    @Test
    @DisplayName("Tests if RoutingInfo is correctly read")
    void testRoutingInfo() {
        Long studyId = studyRepository.insert(new Study().setContact(new Contact().setPerson("test").setEmail("test"))).getStudyId();
        Integer studyGroupId = studyGroupRepository.insert(new StudyGroup().setStudyId(studyId).setTitle("Study Group 1")).getStudyGroupId();
        Integer observationGroupId1 = observationGroupRepository.insert(new ObservationGroup().setStudyId(studyId).setTitle("Observation Group 1").setPurpose("test")).getObservationGroupId();
        Integer observationGroupId2 = observationGroupRepository.insert(new ObservationGroup().setStudyId(studyId).setTitle("Observation Group 2").setPurpose("test")).getObservationGroupId();

        Participant s1 = participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setStudyGroupId(studyGroupId)
                .setObservationGroupIds(Set.of(observationGroupId1, observationGroupId2))
                .setRegistrationToken("RegToken1"));

        Participant s2 = participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setObservationGroupIds(Set.of(observationGroupId1))
                .setRegistrationToken("RegToken2"));

        Participant s3 = participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setStudyGroupId(studyGroupId)
                .setRegistrationToken("RegToken3"));

        Participant s4 = participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setRegistrationToken("RegToken4"));

        var routingInfo1 = participantRepository.getRoutingInfo(studyId, s1.getParticipantId());
        assertThat(routingInfo1.isPresent()).isTrue();
        assertThat(routingInfo1.get().participantId()).isEqualTo(s1.getParticipantId());
        assertThat(routingInfo1.get().studyGroupId()).isEqualTo(studyGroupId);
        assertThat(routingInfo1.get().observationGroupIds()).containsExactlyInAnyOrder(observationGroupId1, observationGroupId2);
        assertThat(routingInfo1.get().participantActive()).isFalse();
        assertThat(routingInfo1.get().studyActive()).isFalse();

        var routingInfo2 = participantRepository.getRoutingInfo(studyId, s2.getParticipantId());
        assertThat(routingInfo2.isPresent()).isTrue();
        assertThat(routingInfo2.get().participantId()).isEqualTo(s2.getParticipantId());
        assertThat(routingInfo2.get().studyGroupId()).isNull();
        assertThat(routingInfo2.get().observationGroupIds()).containsExactlyInAnyOrder(observationGroupId1);
        assertThat(routingInfo2.get().participantActive()).isFalse();
        assertThat(routingInfo2.get().studyActive()).isFalse();

        participantRepository.setStatusByIds(studyId, s3.getParticipantId(), Participant.Status.ACTIVE);

        var routingInfo3 = participantRepository.getRoutingInfo(studyId, s3.getParticipantId());
        assertThat(routingInfo3.isPresent()).isTrue();
        assertThat(routingInfo3.get().participantId()).isEqualTo(s3.getParticipantId());
        assertThat(routingInfo3.get().studyGroupId()).isEqualTo(studyGroupId);
        assertThat(routingInfo3.get().observationGroupIds()).isEmpty();
        assertThat(routingInfo3.get().participantActive()).isTrue();
        assertThat(routingInfo3.get().studyActive()).isFalse();

        participantRepository.setStatusByIds(studyId, s4.getParticipantId(), Participant.Status.ACTIVE);
        studyRepository.setStateById(studyId, Study.Status.ACTIVE);

        var routingInfo4 = participantRepository.getRoutingInfo(studyId, s4.getParticipantId());
        assertThat(routingInfo4.isPresent()).isTrue();
        assertThat(routingInfo4.get().participantId()).isEqualTo(s4.getParticipantId());
        assertThat(routingInfo4.get().studyGroupId()).isNull();
        assertThat(routingInfo4.get().observationGroupIds()).isEmpty();
        assertThat(routingInfo4.get().participantActive()).isTrue();
        assertThat(routingInfo4.get().studyActive()).isTrue();
    }
}
