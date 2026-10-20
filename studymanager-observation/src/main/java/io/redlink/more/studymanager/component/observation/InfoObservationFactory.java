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
import java.time.format.DateTimeParseException;
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
    public static final String FIELD_MESSAGE_ID = "messageId";
    public static final String FIELD_READ = "read";
    public static final Set<String> PRIORITIES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
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

    /** Only datapoints marking a message as read count when validating. */
    @Override
    public Map<String, Object> getValidationFilter() {
        return Map.of(FIELD_READ, true);
    }

    /** A message read in an earlier occurrence stays read. */
    @Override
    public boolean isValidationCumulative() {
        return true;
    }

    /**
     * Messages can not be deleted. New messages get {@value #MSG_SENT_AT} set to now, existing ones keep theirs.
     */
    @Override
    public P prepareUpdate(P previous, P updated) throws ConfigurationValidationException {
        Map<String, Map<?, ?>> before = messagesById(previous);
        Map<String, Map<?, ?>> after = messagesById(updated);
        if (!after.keySet().containsAll(before.keySet())) {
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
            if (m.get("publishAt") != null) {
                try {
                    Instant.parse(String.valueOf(m.get("publishAt")));
                } catch (DateTimeParseException e) {
                    return ValidationIssue.error(messagesValue, "message publishAt must be an ISO-8601 instant");
                }
            }
            if (m.get("attachments") != null && !(m.get("attachments") instanceof List<?>)) {
                return ValidationIssue.error(messagesValue, "message attachments must be a list of ids");
            }
        }
        return ValidationIssue.NONE;
    }
}
