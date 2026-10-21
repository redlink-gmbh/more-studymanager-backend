/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.service;

import io.redlink.more.studymanager.core.component.Component;
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.factory.ObservationFactory;
import io.redlink.more.studymanager.core.io.TimeRange;
import io.redlink.more.studymanager.core.properties.ObservationProperties;
import io.redlink.more.studymanager.core.ui.DataView;
import io.redlink.more.studymanager.core.ui.DataViewInfo;
import io.redlink.more.studymanager.core.validation.ValidationIssue;
import io.redlink.more.studymanager.event.StudyStateChangedEvent;
import io.redlink.more.studymanager.exception.BadRequestException;
import io.redlink.more.studymanager.exception.NotFoundException;
import io.redlink.more.studymanager.model.Observation;
import io.redlink.more.studymanager.model.ParticipantWithObservationProperties;
import io.redlink.more.studymanager.model.Study;
import io.redlink.more.studymanager.model.scheduler.StudyWideEvent;
import io.redlink.more.studymanager.repository.ObservationRepository;
import io.redlink.more.studymanager.sdk.MoreSDK;
import io.redlink.more.studymanager.utils.RandomSchedulerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class ObservationService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final StudyStateService studyStateService;
    private final ObservationRepository repository;

    private final MoreSDK sdk;
    private final ApplicationContext applicationContext;
    private final ObservationAttachmentService attachmentService;

    public ObservationService(StudyStateService studyStateService,
                              ObservationRepository repository,
                              MoreSDK sdk,
                              ApplicationContext applicationContext,
                              ObservationAttachmentService attachmentService) {
        this.studyStateService = studyStateService;
        this.repository = repository;
        this.sdk = sdk;
        this.applicationContext = applicationContext;
        this.attachmentService = attachmentService;
    }

    public Observation addObservation(Observation observation) {
        studyStateService.assertStudyNotInState(observation.getStudyId(), Study.Status.CLOSED);
        return repository.insert(normalize(validate(prepare(observation, null, false))));
    }

    public Observation importObservation(Long studyId, Observation observation) {
        final ObservationFactory factory = factory(observation);
        if (factory == null) {
            throw NotFoundException.ObservationFactory(observation.getType());
        }
        ObservationProperties props = (ObservationProperties) factory.preImport(observation.getProperties());
        observation.setProperties(props);
        if (factory.isStudyWide()) {
            observation.setSchedule(new StudyWideEvent());
        }
        return repository.doImport(studyId, observation);
    }

    public void deleteObservation(Long studyId, Integer observationId) {
        studyStateService.assertStudyNotInState(studyId, Study.Status.CLOSED);
        repository.deleteObservation(studyId, observationId);
        attachmentService.deleteFiles(studyId, observationId);
    }

    public Optional<Observation> getObservation(Long studyId, Integer observationId) {
        try {
            return Optional.ofNullable(repository.getById(studyId, observationId));
        } catch (BadRequestException e) {
            return Optional.empty();
        }
    }

    public List<Observation> listObservations(Long studyId) {
        return repository.listObservations(studyId);
    }

    /**
     * The observations of the study with their properties as adjusted by {@link ObservationFactory#preExport}.
     */
    public List<Observation> listObservationsForExport(Long studyId) {
        List<Observation> observations = listObservations(studyId);
        observations.forEach(o -> getObservationFactory(o).ifPresent(f ->
                o.setProperties((ObservationProperties) f.preExport(o.getProperties()))));
        return observations;
    }

    public List<Observation> listObservationsForGroup(Long studyId, Integer studyGroupId, Collection<Integer> observationGroupIds) {
        return repository.listObservationsForGroup(studyId, studyGroupId, observationGroupIds);
    }

    public Observation updateObservation(Observation observation) {
        studyStateService.assertStudyNotInState(observation.getStudyId(), Study.Status.CLOSED);
        Optional<Observation> previous = getObservation(observation.getStudyId(), observation.getObservationId());
        boolean deletionAllowed = studyStateService.hasStudyState(observation.getStudyId(), Study.Status.CONTENT_DELETION_STATES);
        Observation updatedObservation = repository.updateObservation(normalize(validate(prepare(observation, previous.orElse(null), deletionAllowed))));
        if (deletionAllowed && previous.isPresent()) {
            attachmentService.deleteDropped(observation.getStudyId(), observation.getObservationId(),
                    previous.get().getProperties(), updatedObservation.getProperties());
        }
        repository.removeParticipantsPropertyKey(observation.getStudyId(), observation.getObservationId(), RandomSchedulerUtils.OBSERVATION_SCHEDULE_SEED_KEY);
        if (previous.isPresent() && studyStateService.hasStudyState(observation.getStudyId(), Study.Status.ACTIVE_STATES)) {
            factory(updatedObservation).create(
                    sdk.scopedObservationSDK(updatedObservation.getStudyId(), updatedObservation.getStudyGroupId(), updatedObservation.getObservationId()),
                    updatedObservation.getProperties()
            ).onUpdate(previous.get().getProperties(), updatedObservation.getTitle());
        }
        return updatedObservation;
    }

    @EventListener
    public void handleStudyStateChange(StudyStateChangedEvent event) {
        alignObservationsWithStudyState(event.getStudy());
    }

    protected void alignObservationsWithStudyState(Study study) {
        if (Study.Status.ACTIVE_STATES.contains(study.getStudyState()))
            activateObservationsFor(study);
        else deactivateObservationsFor(study);
    }

    private void activateObservationsFor(Study study) {
        listObservationsFor(study).forEach(Component::activate);
    }

    private void deactivateObservationsFor(Study study) {
        listObservationsFor(study).forEach(Component::deactivate);
    }

    private void validateProperties(List<Observation> observations) {
        for (Observation observation : observations) {
            try {
                factory(observation).validate(observation.getProperties());
            } catch (ConfigurationValidationException e) {
                for (ValidationIssue issue : e.getReport().getIssues()) {
                    issue.setComponentTitle(observation.getTitle());
                }
                throw e;
            }
        }
    }

    public List<io.redlink.more.studymanager.core.component.Observation> listObservationsFor(Study study) {
        List<Observation> observations = listObservations(study.getStudyId());
        List<io.redlink.more.studymanager.core.component.Observation> result = new ArrayList<>();

        validateProperties(observations);

        for (Observation observation : observations) {
            result.add(factory(observation).create(
                    sdk.scopedObservationSDK(observation.getStudyId(), observation.getStudyGroupId(), observation.getObservationId()),
                    observation.getProperties()));
        }

        return result;
    }

    public DataViewInfo[] listDataViews(Long studyId, Integer observationId) {
        var obs = getObservation(studyId, observationId).orElseThrow();

        return factory(obs).create(
                sdk.scopedObservationSDK(obs.getStudyId(), obs.getStudyGroupId(), obs.getObservationId()), obs.getProperties()
        ).listViews();
    }

    public DataView queryData(Long studyId, Integer observationId, String viewName, Integer studyGroupId, Integer participantId, TimeRange timerange) {
        var obs = getObservation(studyId, observationId).orElseThrow();

        return factory(obs).create(
                sdk.scopedObservationSDK(obs.getStudyId(), obs.getStudyGroupId(), obs.getObservationId()), obs.getProperties()
        ).getView(viewName, studyGroupId, participantId, timerange);
    }

    public List<ParticipantWithObservationProperties> getParticipantObservationProperties(Long studyId) {
        return repository.getParticipantObservationProperties(studyId);
    }

    public Optional<ObservationFactory> getObservationFactory(Observation observation) {
        try {
            return Optional.of(applicationContext.getBean(observation.getType(), ObservationFactory.class));
        } catch (NoSuchBeanDefinitionException | BeanNotOfRequiredTypeException e) {
            return Optional.empty();
        }
    }

    /**
     * Ensures the observationFactory for the parsed Observation
     *
     * @param observation
     * @return the factory
     * @throws NotFoundException if the {@link ObservationFactory} for the parsed observation is not present
     */
    private ObservationFactory factory(Observation observation) {
        return getObservationFactory(observation)
                .orElseThrow(() -> new NotFoundException(String.format("ObservationFactory for Observation[study: %s, id:%s, type: %s]",
                        observation.getStudyId(), observation.getObservationId(), observation.getType())));
    }

    /**
     * Study-wide observations have no configurable schedule, so any schedule that still
     * reaches us (stale frontend state, imported study definition) is replaced by a
     * {@link StudyWideEvent}. Storing that marker rather than nothing at all is what lets
     * services that only read the database - the data-gateway above all - expand the
     * observation over the complete study.
     */
    private Observation normalize(Observation observation) {
        if (getObservationFactory(observation).map(ObservationFactory::isStudyWide).orElse(false)) {
            observation.setSchedule(new StudyWideEvent());
        }
        return observation;
    }

    private Observation prepare(Observation observation, Observation previous, boolean deletionAllowed) {
        try {
            observation.setProperties((ObservationProperties) factory(observation).prepareUpdate(
                    previous != null && previous.getType().equals(observation.getType()) ? previous.getProperties() : null,
                    observation.getProperties(),
                    deletionAllowed));
        } catch (ConfigurationValidationException e) {
            throw new BadRequestException(e.getMessage());
        }
        return observation;
    }

    private Observation validate(Observation observation) {
        try {
            factory(observation).validate(observation.getProperties());
        } catch (ConfigurationValidationException e) {
            throw new BadRequestException(e.getMessage());
        }
        return observation;
    }
}
