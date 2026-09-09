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
import io.redlink.more.studymanager.exception.BadStudyStateException;
import io.redlink.more.studymanager.model.EndpointToken;
import io.redlink.more.studymanager.model.Study;
import io.redlink.more.studymanager.repository.IntegrationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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

    private static final Long STUDY_ID = 1L;
    private static final Integer OBSERVATION_ID = 2;

    private final EndpointToken token = new EndpointToken(1, "Integration 1", Instant.ofEpochMilli(0), null);

    @Test
    @DisplayName("Listing tokens must be allowed in every study state (a CLOSED study is still exportable)")
    void testGetTokensIsAllowedForClosedStudy() {
        when(repository.getAllTokens(STUDY_ID, OBSERVATION_ID)).thenReturn(List.of(token));

        assertThat(integrationService.getTokens(STUDY_ID, OBSERVATION_ID)).containsExactly(token);

        //reading must not consult the study state at all
        Mockito.verify(studyStateService, Mockito.never()).assertStudyNotInState(anyLong(), any(Study.Status[].class));
    }

    @Test
    @DisplayName("Reading a single token must be allowed in every study state")
    void testGetTokenIsAllowedForClosedStudy() {
        when(repository.getToken(STUDY_ID, OBSERVATION_ID, 1)).thenReturn(Optional.of(token));

        assertThat(integrationService.getToken(STUDY_ID, OBSERVATION_ID, 1)).contains(token);

        Mockito.verify(studyStateService, Mockito.never()).assertStudyNotInState(anyLong(), any(Study.Status[].class));
    }

    @Test
    @DisplayName("Modifying tokens must still be rejected for a CLOSED study")
    void testModifyingTokensIsRejectedForClosedStudy() {
        when(studyStateService.assertStudyNotInState(STUDY_ID, Study.Status.CLOSED))
                .thenThrow(BadStudyStateException.state());

        assertThatThrownBy(() -> integrationService.addToken(STUDY_ID, OBSERVATION_ID, "Integration 1"))
                .isInstanceOf(BadStudyStateException.class);
        assertThatThrownBy(() -> integrationService.updateToken(STUDY_ID, OBSERVATION_ID, 1, "Integration 1"))
                .isInstanceOf(BadStudyStateException.class);
        assertThatThrownBy(() -> integrationService.deleteToken(STUDY_ID, OBSERVATION_ID, 1))
                .isInstanceOf(BadStudyStateException.class);

        Mockito.verify(repository, Mockito.never()).addToken(any(), any(), any(), any());
        Mockito.verify(repository, Mockito.never()).updateToken(any(), any(), any(), any());
        Mockito.verify(repository, Mockito.never()).deleteToken(any(), any(), any());
    }

}
