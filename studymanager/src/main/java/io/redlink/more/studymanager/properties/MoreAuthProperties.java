/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.properties;

import io.redlink.more.studymanager.model.PlatformRole;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ConfigurationProperties(prefix = "more.auth")
public record MoreAuthProperties(
        ClaimsProperties claims,
        Map<PlatformRole, Set<String>> globalRoles
) {

    public MoreAuthProperties {
        claims = Objects.requireNonNullElse(claims, new ClaimsProperties(null, null));
        globalRoles = Objects.requireNonNullElse(globalRoles, Map.of());
    }


    public record ClaimsProperties(
            String institution,
            String roles
    ) {
        public ClaimsProperties {
            institution = StringUtils.defaultIfEmpty(institution, "org");
            roles = StringUtils.defaultIfEmpty(roles, "realm_access.roles");
        }
    }

}
