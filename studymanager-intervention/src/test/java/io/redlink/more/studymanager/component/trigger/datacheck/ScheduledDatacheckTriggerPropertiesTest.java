/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.component.trigger.datacheck;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import io.redlink.more.studymanager.core.properties.TriggerProperties;
import io.redlink.more.studymanager.core.validation.ValidationIssue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduledDatacheckTriggerPropertiesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void getElasticQueryString() throws IOException {
        TriggerProperties tp = mapper.readValue(
                Resources.getResource("ScheduledDatacheckTriggerProperties.json"),
                TriggerProperties.class
        );
        ScheduledDatacheckTriggerProperties properties = new ScheduledDatacheckTriggerProperties(tp);

        assertTrue(properties.getElasticQueryString().get().contains("observation_id.keyword:17 AND data_y:23"));
        // legacy conditions without queryDataType keep the original query and need no runtime fields
        assertTrue(properties.getElasticQueryString().get().contains("data_x:>75"));
        assertTrue(properties.getRuntimeFields().isEmpty());
    }

    @Test
    void numberConditionsUseRuntimeField() {
        ScheduledDatacheckTriggerProperties properties = properties(
                condition("EPDSsumt1", ">=", "13", "number"),
                condition("EPDSsumt1", "!=", 13, "number"),
                condition("EPDSsumt1", "<", "-2.5", "number"));

        String query = properties.getElasticQueryString().get();
        assertTrue(query.contains("num_data_EPDSsumt1:[\"13.0\" TO *]"), query);
        assertTrue(query.contains("(_exists_:num_data_EPDSsumt1 AND NOT num_data_EPDSsumt1:\"13.0\")"), query);
        assertTrue(query.contains("num_data_EPDSsumt1:{* TO \"-2.5\"}"), query);
        assertFalse(query.contains("data_EPDSsumt1:>="), query);
        assertEquals(Map.of("num_data_EPDSsumt1", "data_EPDSsumt1"), properties.getRuntimeFields());
    }

    @Test
    void stringAndBooleanConditionsStayOnDataField() {
        ScheduledDatacheckTriggerProperties properties = properties(
                condition("submitted", "=", "true", "boolean"),
                condition("seed", "!=", "abc", "string"));

        String query = properties.getElasticQueryString().get();
        assertTrue(query.contains("data_submitted:true"), query);
        assertTrue(query.contains("NOT data_seed:abc"), query);
        assertTrue(properties.getRuntimeFields().isEmpty());
    }

    @Test
    void fieldNamesWithBracketsAreEscaped() {
        ScheduledDatacheckTriggerProperties properties = properties(
                condition("PRAQt1[PRAQ1t1]", ">", "3", "number"),
                condition("PRAQt1[PRAQ2t1]", "=", "A1", "string"));

        String query = properties.getElasticQueryString().get();
        assertTrue(query.contains("num_data_PRAQt1\\[PRAQ1t1\\]:{\"3.0\" TO *}"), query);
        assertTrue(query.contains("data_PRAQt1\\[PRAQ2t1\\]:A1"), query);
        assertEquals(Map.of("num_data_PRAQt1[PRAQ1t1]", "data_PRAQt1[PRAQ1t1]"), properties.getRuntimeFields());
    }

    @Test
    void validateQuery() {
        DatacheckQueryValue prop = new DatacheckQueryValue("queryObject");

        assertEquals(ValidationIssue.NONE, validate(prop, condition("x", ">", 5, null)));
        assertEquals(ValidationIssue.NONE, validate(prop, condition("x", ">=", "13", "number")));
        assertEquals(ValidationIssue.NONE, validate(prop, condition("x", "=", "23.0000000000", "number")));
        assertEquals(ValidationIssue.NONE, validate(prop, condition("submitted", "=", true, "boolean")));

        assertEquals(ValidationIssue.Type.ERROR, validate(prop, condition("x", ">=", "13,5", "number")).getType());
        assertEquals(ValidationIssue.Type.ERROR, validate(prop, condition("x", ">=", "", "number")).getType());
        assertEquals(ValidationIssue.Type.ERROR, validate(prop, condition("x", ">", "a", "string")).getType());
        assertEquals(ValidationIssue.Type.ERROR, validate(prop, condition("x", "=", "yes", "boolean")).getType());
        assertEquals(ValidationIssue.Type.ERROR, validate(prop, condition("x", "=", "1", "date")).getType());
    }

    private static ValidationIssue validate(DatacheckQueryValue prop, Map<String, Object> condition) {
        return ScheduledDatacheckTriggerFactory.validateQuery(prop, queryObject(condition));
    }

    @SafeVarargs
    private ScheduledDatacheckTriggerProperties properties(Map<String, Object>... conditions) {
        TriggerProperties tp = mapper.convertValue(Map.of("queryObject", queryObject(conditions)), TriggerProperties.class);
        return new ScheduledDatacheckTriggerProperties(tp);
    }

    @SafeVarargs
    private static List<Map<String, Object>> queryObject(Map<String, Object>... conditions) {
        Map<String, Object> group = new HashMap<>();
        group.put("nextGroupCondition", null);
        group.put("parameter", List.of(conditions));
        return List.of(group);
    }

    private static Map<String, Object> condition(String property, String operator, Object value, String queryDataType) {
        Map<String, Object> c = new HashMap<>();
        c.put("observationId", 17);
        c.put("observationType", "lime-survey-observation");
        c.put("observationProperty", property);
        c.put("operator", operator);
        c.put("propertyValue", value);
        c.put("queryDataType", queryDataType);
        c.put("editMode", false);
        return c;
    }
}
