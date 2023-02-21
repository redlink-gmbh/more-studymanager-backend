package io.redlink.more.studymanager.service;

import io.redlink.more.studymanager.exception.BadRequestException;
import io.redlink.more.studymanager.exception.DataConstraintException;
import io.redlink.more.studymanager.exception.NotFoundException;
import io.redlink.more.studymanager.model.MoreUser;
import io.redlink.more.studymanager.model.Study;
import io.redlink.more.studymanager.model.StudyRole;
import io.redlink.more.studymanager.model.StudyUserRoles;
import io.redlink.more.studymanager.model.User;
import io.redlink.more.studymanager.repository.StudyAclRepository;
import io.redlink.more.studymanager.repository.StudyRepository;
import io.redlink.more.studymanager.repository.UserRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class StudyService {

    private final StudyRepository studyRepository;
    private final StudyAclRepository aclRepository;
    private final UserRepository userRepo;
    private final InterventionService interventionService;
    private final ObservationService observationService;
    private final ParticipantService participantService;
    private final StudyStateService studyStateService;

    public StudyService(StudyRepository studyRepository, StudyAclRepository aclRepository, UserRepository userRepo,
                        StudyStateService studyStateService, InterventionService interventionService, ObservationService observationService, ParticipantService participantService) {
        this.studyRepository = studyRepository;
        this.aclRepository = aclRepository;
        this.userRepo = userRepo;
        this.studyStateService = studyStateService;
        this.interventionService = interventionService;
        this.observationService = observationService;
        this.participantService = participantService;
    }

    public Study createStudy(Study study, User currentUser) {
        // TODO: Workaround until proper auth is available
        var user = userRepo.save(currentUser);
        var savedStudy = studyRepository.insert(study);
        aclRepository.setRoles(savedStudy.getStudyId(), user.id(), EnumSet.allOf(StudyRole.class), null);
        return getStudy(savedStudy.getStudyId(), user).orElse(savedStudy);
    }

    public List<Study> listStudies(User user) {
        return listStudies(user, EnumSet.allOf(StudyRole.class));
    }

    public List<Study> listStudies(User user, Set<StudyRole> allowedRoles) {
        return studyRepository.listStudiesByAclOrderByModifiedDesc(user, allowedRoles);
    }

    public Optional<Study> getStudy(Long studyId, User user) {
        return (studyRepository.getById(studyId, user));
    }

    public Optional<Study> updateStudy(Study study, User user) {
        studyStateService.assertStudyNotInState(study, Study.Status.CLOSED);
        return studyRepository.update(study, user);
    }

    public void deleteStudy(Long studyId) {
        studyRepository.deleteById(studyId);
    }

    public void setStatus(Long studyId, Study.Status status, User user) {
        Study study = getStudy(studyId, user)
                .orElseThrow(() -> NotFoundException.Study(studyId));
        if (status.equals(Study.Status.DRAFT)) {
            throw BadRequestException.StateChange(study.getStudyState(), Study.Status.DRAFT);
        }
        if (study.getStudyState().equals(Study.Status.CLOSED)) {
            throw BadRequestException.StateChange(Study.Status.CLOSED, status);
        }
        if (study.getStudyState().equals(status)) {
            throw BadRequestException.StateChange(study.getStudyState(), status);
        }

        studyRepository.setStateById(studyId, status);

        studyRepository.getById(studyId).ifPresent(s -> {
            interventionService.alignInterventionsWithStudyState(s);
            participantService.alignParticipantsWithStudyState(s);
            observationService.alignObservationsWithStudyState(s);
        });
    }

    public Map<MoreUser, Set<StudyRole>> getACL(Long studyId) {
        return aclRepository.getACL(studyId);
    }

    public Optional<StudyUserRoles> setRolesForStudy(Long studyId, String userId, Set<StudyRole> roles,
                                                     User currentUser) {
        studyStateService.assertStudyNotInState(studyId, Study.Status.CLOSED);
        //MORE-218: One must not remove oneself as ADMIN
        if (StringUtils.equals(currentUser.id(), userId)
            && !roles.contains(StudyRole.STUDY_ADMIN)) {
            throw DataConstraintException.createNoSelfAdminRemoval(studyId, userId);
        }

        if (roles.isEmpty()) {
            aclRepository.clearRoles(studyId, userId);
            return Optional.empty();
        }

        aclRepository.setRoles(studyId, userId, roles, currentUser.id());
        return getRolesForStudy(studyId, userId);
    }

    public Optional<StudyUserRoles> getRolesForStudy(Long studyId, String userId) {
        return userRepo.getById(userId).map(user ->
                new StudyUserRoles(
                        user,
                        aclRepository.getRoleDetails(studyId, userId)
                )
        );
    }
}
