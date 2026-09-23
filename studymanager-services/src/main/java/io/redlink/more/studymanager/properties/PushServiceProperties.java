/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "push")
public record PushServiceProperties(
        URI baseUrl,
        String apiKey,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("10s") Duration readTimeout,
        @DefaultValue Retry retry
) {

    /**
     * The base-url is the root of the push-service, the {@code /api/v1} prefix comes from the spec.
     */
    public String apiBasePath() {
        return UriComponentsBuilder.fromUri(baseUrl)
                .pathSegment("api", "v1")
                .build()
                .toUriString();
    }

    public record Retry(
            @DefaultValue("5s") Duration initialDelay,
            @DefaultValue("2") double multiplier,
            @DefaultValue("5m") Duration maxDelay,
            @DefaultValue("10") int maxAttempts,
            @DefaultValue("10000") int queueCapacity
    ) {
    }
}
