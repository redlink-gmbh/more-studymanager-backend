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
import io.redlink.more.studymanager.core.exception.ConfigurationValidationException;
import io.redlink.more.studymanager.core.factory.ObservationFactory;
import io.redlink.more.studymanager.core.measurement.Measurement;
import io.redlink.more.studymanager.core.measurement.MeasurementSet;
import io.redlink.more.studymanager.core.properties.ObservationProperties;
import io.redlink.more.studymanager.core.properties.model.ObjectValue;
import io.redlink.more.studymanager.core.properties.model.Value;
import io.redlink.more.studymanager.core.sdk.MoreObservationSDK;
import io.redlink.more.studymanager.core.validation.ConfigurationValidationReport;
import io.redlink.more.studymanager.core.validation.ValidationIssue;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Information shown in the app. Study operators maintain a list of markdown messages (with priority,
 * optional publish time and attachments) and can add or edit them while the study is running.
 */
public class InfoObservationFactory<C extends Observation<P>, P extends ObservationProperties>
        extends ObservationFactory<C, P> {

    public static final String PROP_MESSAGES = "messages";
    /** Set by the server when a message is first stored, never changed afterwards. */
    public static final String MSG_SENT_AT = "sentAt";
    public static final String MSG_PUBLISH_AT = "publishAt";
    /** Publish relative to the activation of the observation for the participant (start of the first occurrence). */
    public static final String MSG_PUBLISH_AFTER = "publishAfter";
    public static final String FIELD_MESSAGE_ID = "messageId";
    public static final String FIELD_READ = "read";
    /** The text the participant saw; only checked to be given, not compared to the message. */
    public static final String FIELD_TEXT = "text";
    /** When the message was shown to the participant. */
    public static final String FIELD_SHOWN_AT = "shownAt";
    public static final Set<String> PRIORITIES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
    private static final Map<String, ChronoUnit> OFFSET_UNITS = Map.of(
            "MINUTE", ChronoUnit.MINUTES, "HOUR", ChronoUnit.HOURS, "DAY", ChronoUnit.DAYS);
    static final int MAX_TEXT_LENGTH = 20_000;

    private static final MeasurementSet measurements = new MeasurementSet(
            "INFO_READ", Set.of(
                    new Measurement(FIELD_MESSAGE_ID, Measurement.Type.STRING),
                    new Measurement(FIELD_READ, Measurement.Type.BOOLEAN))
    );

    private static final ObjectValue messagesValue = new ObjectValue(PROP_MESSAGES);

    static {
        messagesValue
                .setName("observation.factory.info.configProps.messagesName")
                .setDescription("observation.factory.info.configProps.messagesDesc")
                .setDefaultValue(List.of())
                .setValidationFunction(InfoObservationFactory::validateMessages);
    }

    private static final List<Value> properties = List.of(messagesValue);

    @Override
    public String getId() {
        return "info-observation";
    }

    @Override
    public String getTitle() {
        return "observation.factory.info.title";
    }

    @Override
    public String getDescription() {
        return "observation.factory.info.description";
    }

    @Override
    public List<Value> getProperties() {
        return properties;
    }

    @Override
    public InfoObservation create(MoreObservationSDK sdk, ObservationProperties properties) throws ConfigurationValidationException {
        return new InfoObservation(sdk, validate((P) properties));
    }

    @Override
    public MeasurementSet getMeasurementSet() {
        return measurements;
    }

    /** Only datapoints marking a message as read, with the shown text and time given, count when validating. */
    @Override
    public Map<String, Object> getValidationFilter() {
        return Map.of(FIELD_READ, true, FIELD_TEXT, VALUE_PRESENT, FIELD_SHOWN_AT, VALUE_PRESENT);
    }

    /** A message read in an earlier occurrence stays read. */
    @Override
    public boolean isValidationCumulative() {
        return true;
    }

    @Override
    public P prepareUpdate(P previous, P updated) throws ConfigurationValidationException {
        return prepareUpdate(previous, updated, false);
    }

    /**
     * Messages can only be deleted if {@code deletionAllowed} (draft or paused study). New messages get
     * {@value #MSG_SENT_AT} set to now, existing ones keep theirs.
     */
    @Override
    public P prepareUpdate(P previous, P updated, boolean deletionAllowed) throws ConfigurationValidationException {
        Map<String, Map<?, ?>> before = messagesById(previous);
        Map<String, Map<?, ?>> after = messagesById(updated);
        if (!deletionAllowed && !after.keySet().containsAll(before.keySet())) {
            throw new ConfigurationValidationException(ConfigurationValidationReport.of(
                    ValidationIssue.error(messagesValue, "messages can not be deleted")));
        }
        if (after.isEmpty()) {
            return updated;
        }
        String now = Instant.now().toString();
        List<Map<Object, Object>> messages = new ArrayList<>();
        after.forEach((id, msg) -> {
            Map<Object, Object> copy = new LinkedHashMap<>(msg);
            copy.put(MSG_SENT_AT, before.containsKey(id) && before.get(id).get(MSG_SENT_AT) != null
                    ? before.get(id).get(MSG_SENT_AT)
                    : now);
            messages.add(copy);
        });
        updated.put(PROP_MESSAGES, messages);
        return updated;
    }

    /** Messages are study runtime content, they are not part of a study export. */
    @Override
    public P preExport(P values) {
        return withoutMessages(values);
    }

    /** Also drops messages contained in older exports. */
    @Override
    public P preImport(P values) {
        return withoutMessages(values);
    }

    private P withoutMessages(P values) {
        if (values != null) {
            values.put(PROP_MESSAGES, List.of());
        }
        return values;
    }

    /** The messages of the properties by id, in their order. */
    static Map<String, Map<?, ?>> messagesById(ObservationProperties props) {
        Map<String, Map<?, ?>> result = new LinkedHashMap<>();
        if (props != null && props.get(PROP_MESSAGES) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    result.put(String.valueOf(m.get("id")), m);
                }
            }
        }
        return result;
    }

    /**
     * The time a {@value #MSG_PUBLISH_AFTER} message is shown: activation plus the offset (counted from 0), then
     * set to the optional time of day; never before the activation.
     */
    static Instant resolvePublishAfter(Map<?, ?> publishAfter, Instant activation) {
        Map<?, ?> offset = (Map<?, ?>) publishAfter.get("offset");
        ZonedDateTime at = activation.atZone(ZoneId.systemDefault())
                .plus(((Number) offset.get("value")).longValue(), OFFSET_UNITS.get(String.valueOf(offset.get("unit"))));
        if (publishAfter.get("time") != null) {
            at = at.with(LocalTime.parse(String.valueOf(publishAfter.get("time"))));
        }
        return at.toInstant().isBefore(activation) ? activation : at.toInstant();
    }

    static ValidationIssue validateMessages(Object value) {
        if (value == null) {
            return ValidationIssue.NONE;
        }
        if (!(value instanceof List<?> list)) {
            return ValidationIssue.error(messagesValue, "messages must be a list");
        }
        Set<Object> ids = new HashSet<>();
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> m)) {
                return ValidationIssue.error(messagesValue, "each message must be an object");
            }
            if (!(m.get("id") instanceof String id) || id.isBlank() || !ids.add(id)) {
                return ValidationIssue.error(messagesValue, "each message needs a unique id");
            }
            if (!(m.get("text") instanceof String text) || text.isBlank() || text.length() > MAX_TEXT_LENGTH) {
                return ValidationIssue.error(messagesValue, "message text must not be empty or longer than " + MAX_TEXT_LENGTH + " characters");
            }
            if (m.get("priority") != null && !PRIORITIES.contains(m.get("priority"))) {
                return ValidationIssue.error(messagesValue, "message priority must be one of " + PRIORITIES);
            }
            if (m.get(MSG_PUBLISH_AT) != null) {
                try {
                    Instant.parse(String.valueOf(m.get(MSG_PUBLISH_AT)));
                } catch (DateTimeParseException e) {
                    return ValidationIssue.error(messagesValue, "message publishAt must be an ISO-8601 instant");
                }
            }
            if (m.get(MSG_PUBLISH_AFTER) != null) {
                if (m.get(MSG_PUBLISH_AT) != null) {
                    return ValidationIssue.error(messagesValue, "message can not have both publishAt and publishAfter");
                }
                if (!isValidPublishAfter(m.get(MSG_PUBLISH_AFTER))) {
                    return ValidationIssue.error(messagesValue,
                            "message publishAfter needs an offset with a value >= 0 and a unit of " + OFFSET_UNITS.keySet()
                                    + " and an optional time (HH:mm)");
                }
            }
            if (m.get("attachments") != null && !(m.get("attachments") instanceof List<?>)) {
                return ValidationIssue.error(messagesValue, "message attachments must be a list of ids");
            }
        }
        return ValidationIssue.NONE;
    }

    private static boolean isValidPublishAfter(Object value) {
        if (!(value instanceof Map<?, ?> publishAfter)
                || !(publishAfter.get("offset") instanceof Map<?, ?> offset)
                || !(offset.get("value") instanceof Integer || offset.get("value") instanceof Long)
                || ((Number) offset.get("value")).longValue() < 0
                || !OFFSET_UNITS.containsKey(offset.get("unit"))) {
            return false;
        }
        if (publishAfter.get("time") != null) {
            try {
                LocalTime.parse(String.valueOf(publishAfter.get("time")));
            } catch (DateTimeParseException e) {
                return false;
            }
        }
        return true;
    }
}
