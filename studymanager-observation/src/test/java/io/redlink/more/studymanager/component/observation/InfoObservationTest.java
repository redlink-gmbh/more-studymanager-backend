/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.component.observation;

import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.factory.ObservationFactory;
import io.redlink.more.studymanager.core.properties.ObservationProperties;
import io.redlink.more.studymanager.core.sdk.MoreObservationSDK;
import io.redlink.more.studymanager.core.sdk.MorePlatformSDK;
import io.redlink.more.studymanager.core.datavalidity.FieldValue;
import io.redlink.more.studymanager.core.datavalidity.MeasurementSummary;
import io.redlink.more.studymanager.core.datavalidity.ObservationDataState;
import io.redlink.more.studymanager.core.datavalidity.ObservationDataSummary;
import io.redlink.more.studymanager.core.datavalidity.StringMeasurementSummary;
import io.redlink.more.studymanager.core.measurement.Measurement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InfoObservationTest {

    private final InfoObservationFactory<?, ObservationProperties> factory = new InfoObservationFactory<>();
    private final Set<Integer> participants = Set.of(1, 2);
    private MoreObservationSDK sdk;

    @BeforeEach
    void setUp() {
        sdk = mock(MoreObservationSDK.class);
        when(sdk.participantIds(MorePlatformSDK.ParticipantFilter.ACTIVE_ONLY)).thenReturn(participants);
        when(sdk.getObservationId()).thenReturn(7);
    }

    private static Map<String, Object> msg(String id, String text, String priority, String publishAt) {
        Map<String, Object> m = new HashMap<>(Map.of("id", id, "text", text));
        if (priority != null) m.put("priority", priority);
        if (publishAt != null) m.put("publishAt", publishAt);
        return m;
    }

    private static ObservationProperties props(Map<?, ?>... messages) {
        return new ObservationProperties(Map.of("messages", List.of(messages)));
    }

    private void update(ObservationProperties before, ObservationProperties after) {
        factory.create(sdk, after).onUpdate(before, "News");
    }

    @Test
    void newImmediateMessageSendsVisiblePush() {
        when(sdk.participantsWithActiveOccurrence(eq(participants), any())).thenReturn(participants);
        update(props(), props(msg("a", "**Hello** [there](http://x)", "URGENT", null)));
        verify(sdk).sendPushNotification(participants, "News", "Hello there",
                Map.of("key", InfoObservation.PUSH_KEY, "observationId", "7", "messageId", "a"), false, "URGENT");
        verify(sdk, never()).sendPushNotification(any(), any(), any(), any(), eq(true), anyString());
    }

    @Test
    void newImmediateMessageIsSilentWhereTheObservationIsNotActive() {
        when(sdk.participantsWithActiveOccurrence(eq(participants), any())).thenReturn(Set.of(1));
        update(props(), props(msg("a", "x", null, null), msg("b", "y", null, null)));
        verify(sdk).sendPushNotification(eq(Set.of(1)), eq("News"), eq("x"), any(), eq(false), eq("NORMAL"));
        verify(sdk).sendPushNotification(eq(Set.of(1)), eq("News"), eq("y"), any(), eq(false), eq("NORMAL"));
        verify(sdk).sendPushNotification(eq(Set.of(2)), isNull(), isNull(), any(), eq(true), eq("HIGH"));
        // the calendar is only consulted once per update
        verify(sdk).participantsWithActiveOccurrence(eq(participants), any());
    }

    @Test
    void newScheduledMessageSendsSilentPush() {
        update(props(), props(msg("a", "later", null, "2026-10-20T08:00:00Z")));
        verify(sdk).sendPushNotification(eq(participants), isNull(), isNull(), any(), eq(true), eq("HIGH"));
        verify(sdk, never()).sendPushNotification(any(), any(), any(), any(), eq(false), anyString());
    }

    @Test
    void editedMessageSendsSilentPush() {
        update(props(msg("a", "x", null, null)), props(msg("a", "y", null, null)));
        verify(sdk).sendPushNotification(eq(participants), isNull(), isNull(), any(), eq(true), eq("HIGH"));
        verify(sdk, never()).sendPushNotification(any(), any(), any(), any(), eq(false), anyString());
    }

    @Test
    void unchangedMessagesSendNothing() {
        update(props(msg("a", "x", "LOW", null)), props(msg("a", "x", "LOW", null)));
        verify(sdk, never()).sendPushNotification(any(), any(), any(), any(), anyBoolean(), anyString());
    }

    @Test
    void validationRejectsBadMessages() {
        assertThrows(ConfigurationValidationException.class, () -> factory.validate(props(msg("a", "x", null, null), msg("a", "y", null, null))));
        assertThrows(ConfigurationValidationException.class, () -> factory.validate(props(msg("a", "x", "SUPER", null))));
        assertThrows(ConfigurationValidationException.class, () -> factory.validate(props(msg("a", "x", null, "tomorrow"))));
        assertThrows(ConfigurationValidationException.class, () -> factory.validate(props(msg("a", " ", null, null))));
        assertEquals(1, ((List<?>) factory.validate(props(msg("a", "x", "HIGH", "2026-10-20T08:00:00Z"))).get("messages")).size());
    }

    @Test
    void excerptIsTruncatedPlainText() {
        assertEquals("Title text", InfoObservation.excerpt("# Title\n\n_text_"));
        assertEquals(InfoObservation.EXCERPT_LENGTH, InfoObservation.excerpt("a".repeat(500)).length());
    }

    private static ObservationDataSummary readSummary(String... messageIds) {
        MeasurementSummary ms = new MeasurementSummary(new Measurement(InfoObservationFactory.FIELD_MESSAGE_ID, Measurement.Type.STRING));
        ms.setStringResult(new StringMeasurementSummary(Arrays.stream(messageIds).map(id -> new FieldValue<>(id, 1L)).toList()));
        return new ObservationDataSummary(messageIds.length, null, List.of(ms));
    }

    @Test
    void validateDataIsIncompleteUntilAllShownMessagesAreRead() {
        Instant start = Instant.parse("2026-10-20T00:00:00Z");
        Instant end = Instant.parse("2026-10-21T00:00:00Z");
        Map<String, Object> sent = msg("a", "x", null, null);
        sent.put(InfoObservationFactory.MSG_SENT_AT, "2026-10-20T10:00:00Z");
        Map<String, Object> scheduled = msg("b", "y", null, "2026-10-20T08:00:00Z");
        Map<String, Object> sentLater = msg("c", "z", null, null);
        sentLater.put(InfoObservationFactory.MSG_SENT_AT, "2026-10-22T10:00:00Z");
        var obs = factory.create(sdk, props(sent, scheduled, sentLater));

        assertEquals(ObservationDataState.COMPLETE, obs.validateData(start, end, readSummary("a", "b")).state());
        assertEquals(ObservationDataState.INCOMPLETE, obs.validateData(start, end, readSummary("a")).state());
        assertEquals(ObservationDataState.INCOMPLETE, obs.validateData(start, end, readSummary("c")).state());
        assertEquals(ObservationDataState.MISSING, obs.validateData(start, end, null).state());
        assertEquals(ObservationDataState.COMPLETE, factory.create(sdk, props()).validateData(start, end, readSummary()).state());
        // a message sent after the occurrence does not affect it
        assertEquals(ObservationDataState.INCOMPLETE,
                obs.validateData(start, Instant.parse("2026-10-23T00:00:00Z"), readSummary("a", "b")).state());
        assertEquals(Map.of("read", true, "text", ObservationFactory.VALUE_PRESENT, "shownAt", ObservationFactory.VALUE_PRESENT),
                factory.getValidationFilter());
        assertTrue(factory.isValidationCumulative());
    }

    @Test
    void prepareUpdateRejectsDeletionAndKeepsSentAt() {
        ObservationProperties first = factory.prepareUpdate(null, props(msg("a", "x", null, null)));
        Object sentAt = ((Map<?, ?>) ((List<?>) first.get("messages")).get(0)).get(InfoObservationFactory.MSG_SENT_AT);
        assertNotNull(sentAt);

        Map<String, Object> tampered = msg("a", "edited", null, null);
        tampered.put(InfoObservationFactory.MSG_SENT_AT, "2000-01-01T00:00:00Z");
        ObservationProperties second = factory.prepareUpdate(first, props(tampered, msg("b", "y", null, null)));
        List<?> messages = (List<?>) second.get("messages");
        assertEquals(sentAt, ((Map<?, ?>) messages.get(0)).get(InfoObservationFactory.MSG_SENT_AT));
        assertEquals("edited", ((Map<?, ?>) messages.get(0)).get("text"));
        assertNotNull(((Map<?, ?>) messages.get(1)).get(InfoObservationFactory.MSG_SENT_AT));

        assertThrows(ConfigurationValidationException.class, () -> factory.prepareUpdate(second, props(msg("b", "y", null, null))));
    }

    @Test
    void prepareUpdateAllowsDeletionInDraftOrPausedStudy() {
        ObservationProperties first = factory.prepareUpdate(null, props(msg("a", "x", null, null), msg("b", "y", null, null)));
        ObservationProperties second = factory.prepareUpdate(first, props(msg("b", "y", null, null)), true);
        assertEquals(List.of("b"), ((List<?>) second.get("messages")).stream().map(m -> ((Map<?, ?>) m).get("id")).toList());
    }

    private static Map<String, Object> relative(String id, long value, String unit, String time) {
        Map<String, Object> publishAfter = new HashMap<>(Map.of("offset", Map.of("value", value, "unit", unit)));
        if (time != null) publishAfter.put("time", time);
        Map<String, Object> m = msg(id, "x", null, null);
        m.put(InfoObservationFactory.MSG_PUBLISH_AFTER, publishAfter);
        return m;
    }

    @Test
    void validationOfPublishAfter() {
        assertEquals(1, ((List<?>) factory.validate(props(relative("a", 1, "DAY", "09:00"))).get("messages")).size());
        assertEquals(1, ((List<?>) factory.validate(props(relative("a", 0, "HOUR", null))).get("messages")).size());
        assertThrows(ConfigurationValidationException.class, () -> factory.validate(props(relative("a", -1, "DAY", null))));
        assertThrows(ConfigurationValidationException.class, () -> factory.validate(props(relative("a", 1, "WEEK", null))));
        assertThrows(ConfigurationValidationException.class, () -> factory.validate(props(relative("a", 1, "DAY", "9 am"))));
        Map<String, Object> both = relative("a", 1, "DAY", null);
        both.put("publishAt", "2026-10-20T08:00:00Z");
        assertThrows(ConfigurationValidationException.class, () -> factory.validate(props(both)));
    }

    @Test
    void publishAfterIsRelativeToActivation() {
        Instant activation = Instant.parse("2026-10-20T15:30:00Z");
        Map<?, ?> zero = (Map<?, ?>) relative("a", 0, "DAY", null).get(InfoObservationFactory.MSG_PUBLISH_AFTER);
        assertEquals(activation, InfoObservationFactory.resolvePublishAfter(zero, activation));

        Map<?, ?> nextMorning = (Map<?, ?>) relative("a", 1, "DAY", "09:00").get(InfoObservationFactory.MSG_PUBLISH_AFTER);
        assertEquals(activation.atZone(ZoneId.systemDefault()).plusDays(1).with(LocalTime.of(9, 0)).toInstant(),
                InfoObservationFactory.resolvePublishAfter(nextMorning, activation));

        // a time of day before the activation is clamped to the activation
        Map<?, ?> sameDayEarly = (Map<?, ?>) relative("a", 0, "DAY", "00:00").get(InfoObservationFactory.MSG_PUBLISH_AFTER);
        assertEquals(activation, InfoObservationFactory.resolvePublishAfter(sameDayEarly, activation));
    }

    @Test
    void validateDataResolvesPublishAfterPerParticipant() {
        var obs = factory.create(sdk, props(relative("a", 24, "HOUR", null)));
        Instant start = Instant.parse("2026-10-23T00:00:00Z");
        Instant end = Instant.parse("2026-10-24T00:00:00Z");
        // activated two days earlier: the message was shown by now and is not read
        assertEquals(ObservationDataState.INCOMPLETE,
                obs.validateData(() -> Instant.parse("2026-10-21T00:00:00Z"), start, end, readSummary()).state());
        // activated (late joiner) at the start of this occurrence: the message is only shown afterwards
        assertEquals(ObservationDataState.COMPLETE,
                obs.validateData(() -> start, start, end, readSummary()).state());
    }

    @Test
    void messagesAreNotExportedOrImported() {
        assertEquals(List.of(), factory.preExport(props(msg("a", "x", null, null))).get("messages"));
        assertEquals(List.of(), factory.preImport(props(msg("a", "x", null, null))).get("messages"));
    }
}
