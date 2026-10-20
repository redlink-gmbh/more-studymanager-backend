/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.component.observation;

import io.redlink.more.studymanager.core.component.Observation;
import io.redlink.more.studymanager.core.datavalidity.FieldValue;
import io.redlink.more.studymanager.core.datavalidity.ObservationDataState;
import io.redlink.more.studymanager.core.datavalidity.ObservationDataSummary;
import io.redlink.more.studymanager.core.datavalidity.ObservationValidationResult;
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.properties.ObservationProperties;
import io.redlink.more.studymanager.core.sdk.MoreObservationSDK;
import io.redlink.more.studymanager.core.sdk.MorePlatformSDK;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class InfoObservation<C extends ObservationProperties> extends Observation<C> {

    public static final String PUSH_KEY = "INFO_OBSERVATION_UPDATED";
    static final int EXCERPT_LENGTH = 100;

    public InfoObservation(MoreObservationSDK sdk, C properties) throws ConfigurationValidationException {
        super(sdk, properties);
    }

    /**
     * New messages without a publish time are announced with a visible push. Scheduled or edited messages only
     * send a silent push so the app re-fetches its config; the app itself shows scheduled messages at their publishAt.
     */
    @Override
    public void onUpdate(ObservationProperties previous, String title) {
        Map<String, Map<?, ?>> before = InfoObservationFactory.messagesById(previous);
        Map<String, Map<?, ?>> after = InfoObservationFactory.messagesById(properties);
        if (before.equals(after)) {
            return;
        }
        Set<Integer> participants = sdk.participantIds(MorePlatformSDK.ParticipantFilter.ACTIVE_ONLY);
        boolean silentUpdate = false;
        for (Map.Entry<String, Map<?, ?>> e : after.entrySet()) {
            Map<?, ?> msg = e.getValue();
            if (!before.containsKey(e.getKey())) {
                if (msg.get("publishAt") == null) {
                    sdk.sendPushNotification(participants, title, excerpt(String.valueOf(msg.get("text"))),
                            data(e.getKey()), false, Objects.toString(msg.get("priority"), "NORMAL"));
                } else {
                    silentUpdate = true;
                }
            } else if (!msg.equals(before.get(e.getKey()))) {
                silentUpdate = true;
            }
        }
        if (silentUpdate) {
            sdk.sendPushNotification(participants, null, null, data(null), true, "HIGH");
        }
    }

    /**
     * Complete if every message shown by the end of the occurrence (publishAt, otherwise sentAt) has a datapoint
     * with its messageId and read=true, otherwise incomplete. So a complete occurrence becomes incomplete once a
     * new message is sent, until it is read. The summary only contains read datapoints from the start of the study
     * (see {@link InfoObservationFactory#getValidationFilter()} and {@link InfoObservationFactory#isValidationCumulative()}).
     */
    @Override
    public ObservationValidationResult validateData(Instant start, Instant end, ObservationDataSummary summary) {
        if (summary == null) {
            return new ObservationValidationResult(false, ObservationDataState.MISSING);
        }
        Set<String> expected = InfoObservationFactory.messagesById(properties).entrySet().stream()
                .filter(e -> shownAt(e.getValue()).isBefore(end))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Set<String> read = summary.measurements() == null ? Set.of() : summary.measurements().stream()
                .filter(m -> InfoObservationFactory.FIELD_MESSAGE_ID.equals(m.getMeasurement().getId()))
                .filter(m -> m.getStringResult() != null)
                .flatMap(m -> m.getStringResult().values().stream())
                .filter(v -> v.value() != null && v.count() > 0)
                .map(FieldValue::value)
                .collect(Collectors.toSet());
        return new ObservationValidationResult(false,
                read.containsAll(expected) ? ObservationDataState.COMPLETE : ObservationDataState.INCOMPLETE);
    }

    private static Instant shownAt(Map<?, ?> msg) {
        Object at = msg.get("publishAt") != null ? msg.get("publishAt") : msg.get(InfoObservationFactory.MSG_SENT_AT);
        return at == null ? Instant.EPOCH : Instant.parse(String.valueOf(at));
    }

    private Map<String, String> data(String messageId) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("key", PUSH_KEY);
        data.put("observationId", String.valueOf(sdk.getObservationId()));
        if (messageId != null) {
            data.put("messageId", messageId);
        }
        return data;
    }

    /**
     * Plain-text preview of a markdown message for the notification body.
     */
    static String excerpt(String markdown) {
        String plain = markdown
                .replaceAll("!?\\[([^\\]]*)]\\([^)]*\\)", "$1") // links and images -> label
                .replaceAll("[*_`#>~]", "")
                .replaceAll("\\s+", " ")
                .strip();
        return plain.length() <= EXCERPT_LENGTH ? plain : plain.substring(0, EXCERPT_LENGTH - 1) + "…";
    }
}
