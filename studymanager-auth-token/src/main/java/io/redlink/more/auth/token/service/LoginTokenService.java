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
import io.redlink.more.auth.token.event.ParticipantUpdateEvent;
import io.redlink.more.auth.token.repository.LoginTokenRepository;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class LoginTokenService implements ApplicationListener<ParticipantUpdateEvent> {

    private static final Logger log = LoggerFactory.getLogger(LoginTokenService.class);

    private final LoginTokenRepository loginTokenRepository;
    private final LoginTokenProperties properties;

    public LoginTokenService(LoginTokenRepository loginTokenRepository, LoginTokenProperties properties) {
        this.loginTokenRepository = loginTokenRepository;
        this.properties = properties;
    }

    @Override
    public void onApplicationEvent(ParticipantUpdateEvent event) {
        switch (event.getAction()) {
            case DELETE -> loginTokenRepository.deleteLoginTokens(event.getStudyId(), event.getParticipantId());
        }
    }

    private String hashToken(String input) {
        if (input == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(properties.getHashAlgorithm());
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Hash algorithm {} not found", properties.getHashAlgorithm());
            return input;
        }
    }
}
