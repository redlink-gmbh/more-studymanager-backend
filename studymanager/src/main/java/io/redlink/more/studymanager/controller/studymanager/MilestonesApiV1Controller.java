package io.redlink.more.studymanager.controller.studymanager;

import io.redlink.more.studymanager.api.v1.model.CreateMilestoneRequestDTO;
import io.redlink.more.studymanager.api.v1.model.CreateParticipantMilestoneRequestDTO;
import io.redlink.more.studymanager.api.v1.model.MilestoneDTO;
import io.redlink.more.studymanager.api.v1.model.ParticipantMilestoneDTO;
import io.redlink.more.studymanager.api.v1.webservices.MilestonesApi;
import io.redlink.more.studymanager.audit.Audited;
import io.redlink.more.studymanager.controller.RequiresStudyRole;
import io.redlink.more.studymanager.model.StudyRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class MilestonesApiV1Controller implements MilestonesApi {

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<List<MilestoneDTO>> listMilestones(Long studyId) {
        // TODO: implement
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<MilestoneDTO> createMilestone(Long studyId, CreateMilestoneRequestDTO createMilestoneRequestDTO) {
        // TODO: implement
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<MilestoneDTO> getMilestone(Long studyId, Integer milestoneId) {
        // TODO: implement
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<MilestoneDTO> updateMilestone(Long studyId, Integer milestoneId, MilestoneDTO milestoneDTO) {
        // TODO: implement
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    @Audited
    public ResponseEntity<Void> deleteMilestone(Long studyId, Integer milestoneId) {
        // TODO: implement
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<List<ParticipantMilestoneDTO>> listParticipantMilestones(Long studyId, Integer participantId) {
        // TODO: implement
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<ParticipantMilestoneDTO> createParticipantMilestone(Long studyId, Integer participantId, CreateParticipantMilestoneRequestDTO createParticipantMilestoneRequestDTO) {
        // TODO: implement
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<ParticipantMilestoneDTO> getParticipantMilestone(Long studyId, Integer participantId, Integer milestoneId) {
        // TODO: implement
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<ParticipantMilestoneDTO> updateParticipantMilestone(Long studyId, Integer participantId, Integer milestoneId, ParticipantMilestoneDTO participantMilestoneDTO) {
        // TODO: implement
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    @RequiresStudyRole({StudyRole.STUDY_ADMIN, StudyRole.STUDY_OPERATOR})
    @Audited
    public ResponseEntity<Void> deleteParticipantMilestone(Long studyId, Integer participantId, Integer milestoneId) {
        // TODO: implement
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
