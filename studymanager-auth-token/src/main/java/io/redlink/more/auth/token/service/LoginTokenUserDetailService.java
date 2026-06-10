/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Oesterreichische Vereinigung zur
 * Foerderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.auth.token.service;

import io.redlink.more.auth.token.model.LoginTokenUserDetails;
import io.redlink.more.auth.token.repository.LoginTokenUserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class LoginTokenUserDetailService implements UserDetailsService {

    public static final String APP_ROLE = "APP";

    private final LoginTokenUserRepository loginTokenUserRepository;

    LoginTokenUserDetailService(LoginTokenUserRepository loginTokenUserRepository) {
        this.loginTokenUserRepository = loginTokenUserRepository;
    }

    @Override
    public LoginTokenUserDetails loadUserByUsername(String apiId) throws UsernameNotFoundException {
        final Optional<LoginTokenUserDetails> gatewayUserDetails = this.loginTokenUserRepository.findByApiId(apiId, Set.of(APP_ROLE));

        return gatewayUserDetails.orElseThrow(
                () -> new UsernameNotFoundException(String.format("ApiId [%s] not found", apiId))
        );
    }

}
