/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.configuration;

import io.redlink.more.studymanager.properties.PushServiceProperties;
import io.redlink.more.studymanager.push.client.ApiClient;
import io.redlink.more.studymanager.push.client.api.NotificationsApi;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PushServiceProperties.class)
public class PushServiceConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PushServiceConfiguration.class);

    @Bean
    public NotificationsApi pushNotificationsApi(PushServiceProperties props) {
        if (StringUtils.isBlank(props.apiKey())) {
            log.warn("No push-service api-key configured, notifications will be rejected by {}", props.apiBasePath());
        }

        // Explicit timeouts: a hung push-service must never block a caller-thread indefinitely.
        final RestClient restClient = RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(
                        ClientHttpRequestFactorySettings.defaults()
                                .withConnectTimeout(props.connectTimeout())
                                .withReadTimeout(props.readTimeout())))
                .build();

        final ApiClient apiClient = new ApiClient(restClient);
        apiClient.setBasePath(props.apiBasePath());
        apiClient.setApiKey(props.apiKey());

        log.info("Push-notifications are delivered via {}", props.apiBasePath());
        return new NotificationsApi(apiClient);
    }
}
