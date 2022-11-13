package io.redlink.more.studymanager.repository;

import io.redlink.more.studymanager.model.Participant;
import io.redlink.more.studymanager.model.Study;
import io.redlink.more.studymanager.model.StudyGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test-containers-flyway")
class ParticipantRepositoryTest {
    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @BeforeEach
    void deleteAll() {
        participantRepository.clear();
    }

    @Test
    @DisplayName("Participant is inserted and returned")
    public void testInsert() {
        Long studyId = studyRepository.insert(new Study()).getStudyId();
        Integer studyGroupId = studyGroupRepository.insert(new StudyGroup()
                .setStudyId(studyId)).getStudyGroupId();

        Participant participant = new Participant()
                .setAlias("participant x")
                .setStudyGroupId(studyGroupId)
                .setStudyId(studyId)
                .setRegistrationToken("TEST123");

        Participant participantResponse = participantRepository.insert(participant);

        assertThat(participantResponse.getAlias()).isEqualTo(participant.getAlias());
        assertThat(participantResponse.getStatus()).isEqualTo(Participant.Status.NEW);
        assertThat(participantResponse.getParticipantId()).isNotNull();

        Participant update = participantResponse.setAlias("new participant x");

        Participant updated = participantRepository.update(update);

        Participant queried = participantRepository.getByIds(participantResponse.getStudyId(), participantResponse.getParticipantId());

        assertThat(queried.getAlias()).isEqualTo(updated.getAlias());
        assertThat(queried.getStudyId()).isEqualTo(updated.getStudyId());
        assertThat(queried.getCreated()).isEqualTo(updated.getCreated());
        assertThat(queried.getStatus()).isEqualTo(updated.getStatus());

        assertThat(update.getAlias()).isEqualTo(updated.getAlias());
        assertThat(participantResponse.getStudyId()).isEqualTo(updated.getStudyId());
        assertThat(participantResponse.getCreated()).isEqualTo(updated.getCreated());
        assertThat(participantResponse.getModified().getTime()).isLessThan(updated.getModified().getTime());
    }

    @Test
    @DisplayName("Studies are deleted and listed correctly")
    public void testListAndDelete() {
        Long studyId = studyRepository.insert(new Study()).getStudyId();

        Participant s1 = participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setRegistrationToken("TEST123"));
        Participant s2 = participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setRegistrationToken("TEST456"));
        Participant s3 = participantRepository.insert(new Participant()
                .setStudyId(studyId)
                .setRegistrationToken("TEST789"));

        assertThat(participantRepository.listParticipants(studyId).size()).isEqualTo(3);
        participantRepository.deleteParticipant(studyId, s1.getParticipantId());
        assertThat(participantRepository.listParticipants(studyId).size()).isEqualTo(2);
        participantRepository.deleteParticipant(studyId, s2.getParticipantId());
        assertThat(participantRepository.listParticipants(studyId).size()).isEqualTo(1);
        participantRepository.deleteParticipant(studyId, s2.getParticipantId());
        assertThat(participantRepository.listParticipants(studyId).size()).isEqualTo(1);
        participantRepository.deleteParticipant(studyId, s3.getParticipantId());
        assertThat(participantRepository.listParticipants(studyId).size()).isEqualTo(0);
    }

    @Test
    @DisplayName("Participant states are set correctly")
    public void testSetState() {
        Long studyId = studyRepository.insert(new Study()).getStudyId();

        Participant participant = participantRepository.insert(new Participant().setStudyId(studyId).setRegistrationToken("TEST123"));
        assertThat(participant.getStatus()).isEqualTo(Participant.Status.NEW);

        participant = participantRepository.getByIds(studyId, participant.getParticipantId());
        assertThat(participant.getStatus()).isEqualTo(Participant.Status.NEW);

        participantRepository.setStatusByIds(studyId, participant.getParticipantId(), Participant.Status.ACTIVE);

        participant = participantRepository.getByIds(studyId, participant.getParticipantId());
        assertThat(participant.getStatus()).isEqualTo(Participant.Status.ACTIVE);
    }

    @Test
    @DisplayName("Participants study group must be undefined")
    public void testUndefinedStudyGroup() {
        Long studyId = studyRepository.insert(new Study()).getStudyId();
        Participant participant = participantRepository
                .insert(new Participant().setStudyId(studyId).setRegistrationToken("abc"));
        assertThat(participant.getStudyGroupId()).isNull();
    }

}
