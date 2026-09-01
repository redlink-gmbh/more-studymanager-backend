/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.service;

import io.redlink.more.studymanager.event.StudyStateChangedEvent;
import io.redlink.more.studymanager.model.Study;
import io.redlink.more.studymanager.repository.IntegrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class IntegrationServiceTest {
    @Mock
    StudyStateService studyStateService;
    @Mock
    IntegrationRepository repository;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    IntegrationService integrationService;

    @Test
    void testHandleStudyStateChange() {
        Study study = new Study()
                .setStudyId(1L)
                .setTitle("Test study")
                .setStudyState(Study.Status.ACTIVE);

        integrationService.handleStudyStateChange(new StudyStateChangedEvent(this, study, Study.Status.DRAFT));
        Mockito.verify(repository, Mockito.never()).clearForStudyId(anyLong());
        study.setStudyState(Study.Status.CLOSED);
        integrationService.handleStudyStateChange(new StudyStateChangedEvent(this, study, Study.Status.ACTIVE));
        Mockito.verify(repository, Mockito.times(1)).clearForStudyId(eq(study.getStudyId()));

    }

}
