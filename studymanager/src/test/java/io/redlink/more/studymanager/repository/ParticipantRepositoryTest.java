package io.redlink.more.studymanager.repository;

import io.redlink.more.studymanager.ApplicationTest;
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
class ParticipantRepositoryTest extends ApplicationTest {
    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired StudyGroupRepository studyGroupRepository;

    @BeforeEach
    void deleteAll() {
        participantRepository.clear();
        studyRepository.clear();
        studyRepository.clear();
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
                .setStudyId(studyId);

        Participant participantResponse = participantRepository.insert(participant);

        assertThat(participantResponse.getAlias()).isEqualTo(participant.getAlias());
        assertThat(participantResponse.getStatus()).isEqualTo(Participant.Status.NEW);
        assertThat(participantResponse.getParticipantId()).isNotNull();

        Participant update = participantResponse.setAlias("new participant x")
                .setStatus(Participant.Status.REGISTERED);

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
        assertThat(participantResponse.getStatus()).isEqualTo(updated.getStatus());
    }

    @Test
    @DisplayName("Studies are deleted and listed correctly")
    public void testListAndDelete() {
        Long studyId = studyRepository.insert(new Study()).getStudyId();

        Participant s1 = participantRepository.insert(new Participant()
                .setStudyId(studyId));
        Participant s2 = participantRepository.insert(new Participant()
                .setStudyId(studyId));
        Participant s3 = participantRepository.insert(new Participant()
                .setStudyId(studyId));

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

        Participant participant = participantRepository.insert(new Participant().setStudyId(studyId));
        assertThat(participant.getStatus()).isEqualTo(Participant.Status.NEW);

        participant = participantRepository.getByIds(studyId, participant.getParticipantId());
        assertThat(participant.getStatus()).isEqualTo(Participant.Status.NEW);

        participantRepository.update(new Participant().
                setStudyId(studyId)
                .setParticipantId(participant.getParticipantId())
                .setStatus(Participant.Status.REGISTERED));

        participant = participantRepository.getByIds(studyId, participant.getParticipantId());
        assertThat(participant.getStatus()).isEqualTo(Participant.Status.REGISTERED);
    }

}
