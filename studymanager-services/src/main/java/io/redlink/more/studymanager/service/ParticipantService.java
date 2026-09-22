/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.service;

import io.redlink.more.studymanager.event.PushNotificationEvent;
import io.redlink.more.studymanager.event.StudyStateChangedEvent;
import io.redlink.more.studymanager.model.Participant;
import io.redlink.more.studymanager.model.Study;
import io.redlink.more.studymanager.model.generator.RandomTokenGenerator;
import io.redlink.more.studymanager.push.client.model.MessageType;
import io.redlink.more.studymanager.push.client.model.Priority;
import io.redlink.more.studymanager.repository.ParticipantRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
public class ParticipantService {

    private static final String STUDY_UPDATE_TITLE = "Your Study has a new update";

    private final StudyStateService studyStateService;
    private final ParticipantRepository participantRepository;
    private final ElasticService elasticService;
    private final ApplicationAccessService applicationAccessService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ParticipantService(
            StudyStateService studyStateService, ParticipantRepository repository, ElasticService elasticService,
            ApplicationAccessService applicationAccessService, ApplicationEventPublisher applicationEventPublisher) {
        this.studyStateService = studyStateService;
        this.participantRepository = repository;
        this.elasticService = elasticService;
        this.applicationAccessService = applicationAccessService;
        this.applicationEventPublisher = applicationEventPublisher;
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
        applicationAccessService.deleteApplicationAccess(studyId, participantId);
        if (Boolean.TRUE.equals(includeData)) {
            elasticService.removeDataForParticipant(studyId, participantId);
        }
    }

    public Participant updateParticipant(Participant participant) {
        studyStateService.assertStudyNotInState(participant.getStudyId(), Study.Status.CLOSED);
        return participantRepository.update(participant);
    }

    @EventListener
    @Transactional
    public void handleStudyStateChange(StudyStateChangedEvent event) {
        alignParticipantsWithStudyState(event.getStudy());
        notifyParticipantsAboutStudyState(event.getStudy(), event.getPreviousState());
    }

    private void notifyParticipantsAboutStudyState(Study study, Study.Status previousState) {
        notifyAboutStudyState(
                study.getStudyId(),
                listParticipants(study.getStudyId()).stream().map(Participant::getParticipantId).toList(),
                previousState,
                study.getStudyState());
    }

    /**
     * Let participants know their study moved on - silently, the app picks it up on its own terms.
     */
    public void notifyAboutStudyState(long studyId, List<Integer> participantIds,
                                      Study.Status previousState, Study.Status newState) {
        if (participantIds.isEmpty()) {
            return;
        }
        applicationEventPublisher.publishEvent(new PushNotificationEvent(
                this, studyId, participantIds,
                STUDY_UPDATE_TITLE,
                "Your study was updated. For more information, please launch the app!",
                Map.of("key", "STUDY_STATE_CHANGED",
                        "oldState", previousState.toAppState(),
                        "newState", newState.toAppState()),
                MessageType.BACKGROUND, Priority.NORMAL));
    }


    private void alignParticipantsWithStudyState(Study study) {
        switch (study.getStudyState()) {
            case CLOSED -> participantRepository.cleanupParticipants(study.getStudyId());
            case DRAFT -> participantRepository.resetParticipants(study.getStudyId(), RandomTokenGenerator::generate);
        }
    }


    public void setStatus(Long studyId, Integer participantId, Participant.Status status) {
        studyStateService.assertStudyNotInState(studyId, Study.Status.CLOSED);
        participantRepository.setStatusByIds(studyId, participantId, status);
        if (EnumSet.of(Participant.Status.ABANDONED, Participant.Status.KICKED_OUT, Participant.Status.LOCKED)
                .contains(status)) {
            participantRepository.cleanupParticipant(studyId, participantId);
            applicationAccessService.deleteApplicationAccess(studyId, participantId);
        }
    }
}
