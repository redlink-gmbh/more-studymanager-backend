/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.service;

import io.redlink.more.studymanager.model.Intervention;
import io.redlink.more.studymanager.model.Observation;
import io.redlink.more.studymanager.model.Participant;
import io.redlink.more.studymanager.model.Study;
import io.redlink.more.studymanager.model.generator.RandomTokenGenerator;
import io.redlink.more.studymanager.repository.InterventionRepository;
import io.redlink.more.studymanager.repository.ObservationRepository;
import io.redlink.more.studymanager.repository.ParticipantRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipantService {

    private final StudyStateService studyStateService;
    private final ObservationRepository observationRepository;
    private final InterventionRepository interventionRepository;
    private final ParticipantRepository participantRepository;
    private final ElasticService elasticService;

    public ParticipantService(
            ParticipantRepository repository,
            StudyStateService studyStateService,
            ObservationRepository observationRepository,
            InterventionRepository interventionRepository,
            ElasticService elasticService
    ) {
        this.participantRepository = repository;
        this.studyStateService = studyStateService;
        this.observationRepository = observationRepository;
        this.interventionRepository = interventionRepository;
        this.elasticService = elasticService;
    }

    public Participant createParticipant(Participant participant) {
        studyStateService.assertStudyNotInState(participant.getStudyId(), Study.Status.CLOSED);
        participant.setRegistrationToken(RandomTokenGenerator.generate());
        return participantRepository.insert(participant);
    }

    public List<Participant> listParticipants(Long studyId) {
        return participantRepository.listParticipants(studyId);
    }

    public List<Participant> listParticipantsForClosing() {
        return participantRepository.listParticipantsForClosing();
    }

    public Participant getParticipant(Long studyId, Integer participantId) {
        return participantRepository.getByIds(studyId, participantId);
    }

    public void deleteParticipant(Long studyId, Integer participantId, Boolean includeData) {
        studyStateService.assertStudyNotInState(studyId, Study.Status.CLOSED);
        participantRepository.deleteParticipant(studyId, participantId);
        if(Boolean.TRUE.equals(includeData)) {
               elasticService.removeDataForParticipant(studyId, participantId);
        }
    }

    public Participant updateParticipant(Participant participant) {
        studyStateService.assertStudyNotInState(participant.getStudyId(), Study.Status.CLOSED);
        return participantRepository.update(participant);
    }

    @Transactional
    public void alignParticipantsWithStudyState(Study study) {
        if (EnumSet.of(Study.Status.CLOSED).contains(study.getStudyState())) {
            participantRepository.cleanupParticipants(study.getStudyId());
        }
        if (EnumSet.of(Study.Status.DRAFT).contains(study.getStudyState())) {
            participantRepository.resetParticipants(study.getStudyId(), RandomTokenGenerator::generate);
        }
    }

    public void setStatus(Long studyId, Integer participantId, Participant.Status status) {
        studyStateService.assertStudyNotInState(studyId, Study.Status.CLOSED);
        participantRepository.setStatusByIds(studyId, participantId, status);
        if (EnumSet.of(Participant.Status.ABANDONED, Participant.Status.KICKED_OUT, Participant.Status.LOCKED)
                .contains(status)) {
            participantRepository.cleanupParticipant(studyId, participantId);
        }
    }

    /**
     * Lists observations for the referenced participant
     * @param studyId the study to list observations
     * @param participantId the participant id
     * @return the list of observations or an empty list if none or the participant was not found
     */
    @Transactional(readOnly = true)
    public List<Observation> listObservations(Long studyId, Integer participantId) {
        return listObservations(getParticipant(studyId, participantId));
    }

    /**
     * Lists observations for the parsed participant
     * @param participant the participant
     * @return the list of observations or an empty list if <code>null</code> is parsed as participant
     */
    public List<Observation> listObservations(Participant participant) {
        if(participant == null) {
            return Collections.emptyList();
        }
        return observationRepository.listObservationsForGroup(
                participant.getStudyId(),
                participant.getStudyGroupId(),
                participant.getObservationGroupIds()
        );
    }
    /**
     * Lists interventions for the referenced participant
     * @param studyId the study to list observations
     * @param participantId the participant id
     * @return the list of interventions or an empty list if none or the participant was not found
     */
    @Transactional(readOnly = true)
    public List<Intervention> listIntervations(Long studyId, Integer participantId) {
        return listInterventions(getParticipant(studyId, participantId));
    }

    /**
     * Lists interventions for the parsed participant
     * @param participant the participant
     * @return the list of interventions or an empty list if <code>null</code> is parsed as participant
     */
    public List<Intervention> listInterventions(Participant participant) {
        if(participant == null) {
            return Collections.emptyList();
        }
        return interventionRepository.listInterventionsForGroup(
                participant.getStudyId(),
                participant.getStudyGroupId(),
                participant.getObservationGroupIds()
        );
    }
}
