/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.auth.token.service;

import io.redlink.more.auth.token.configuration.LoginTokenProperties;
import io.redlink.more.auth.token.event.ParticipantUpdateAction;
import io.redlink.more.auth.token.event.ParticipantUpdateEvent;
import io.redlink.more.auth.token.repository.LoginTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginTokenServiceTest {

    @Mock
    private LoginTokenRepository loginTokenRepository;

    private LoginTokenProperties properties;
    private LoginTokenService loginTokenService;

    private final String hashAlgorithm = "SHA-256";

    @BeforeEach
    void setUp() {
        properties = new LoginTokenProperties();
        properties.setHashAlgorithm(hashAlgorithm);
        loginTokenService = new LoginTokenService(loginTokenRepository, properties);
    }

    @Test
    void testValidateConfigurationSuccess() {
        assertDoesNotThrow(() -> properties.validateConfiguration());
    }

    @Test
    void testValidateConfigurationInvalidHashAlgorithm() {
        properties.setHashAlgorithm("INVALID-ALGORITHM");
        assertThrows(IllegalStateException.class, () -> properties.validateConfiguration());
    }

    @Test
    void testOnApplicationEventDelete() {
        Long studyId = 1L;
        Integer participantId = 10;
        ParticipantUpdateEvent event = new ParticipantUpdateEvent(this, studyId, participantId, ParticipantUpdateAction.DELETE);

        loginTokenService.onApplicationEvent(event);

        verify(loginTokenRepository).deleteLoginTokens(studyId, participantId);
    }

}
