/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.core.sdk;

import io.redlink.more.studymanager.core.io.TimeRange;
import io.redlink.more.studymanager.core.properties.ObservationProperties;
import io.redlink.more.studymanager.core.ui.DataViewData;
import io.redlink.more.studymanager.core.ui.ViewConfig;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MoreObservationSDK extends MorePlatformSDK {

    void mergePropertiesForParticipant(Integer participantId, ObservationProperties properties);

    Optional<ObservationProperties> getPropertiesForParticipant(Integer participantId);

    void removePropertiesForParticipant(Integer participantId);

    void storeDataPoint(Integer participantId, String observationType, Map data);

    DataViewData queryData(ViewConfig viewConfig, Integer studyGroupId, Integer participantId, TimeRange timerange);

    int getObservationId();

    /**
     * Sends a push notification to the given participants.
     *
     * @param background {@code true} sends a silent data-only message (title and message are dropped)
     * @param priority   one of {@code URGENT, HIGH, NORMAL, LOW}
     */
    void sendPushNotification(Set<Integer> participantIds, String title, String message,
                              Map<String, String> data, boolean background, String priority);
}
