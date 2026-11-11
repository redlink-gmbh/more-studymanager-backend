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
import io.redlink.more.studymanager.core.io.RuntimeFieldKind;
import io.redlink.more.studymanager.core.io.RuntimeFieldSpec;
import io.redlink.more.studymanager.core.properties.TriggerProperties;
import io.redlink.more.studymanager.core.validation.ValidationIssue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        assertTrue(properties.getElasticQueryString().get().contains("observation_id.keyword:17 AND data_y:\"23\""));
        // legacy conditions without queryDataType keep the original query and need no runtime fields
        assertTrue(properties.getElasticQueryString().get().contains("data_x:>75"));
        assertTrue(properties.getRuntimeFields().isEmpty());
    }

    @Test
    void longConditionsUseLongRuntimeField() {
        ScheduledDatacheckTriggerProperties properties = properties(
                condition("EPDSsumt1", ">=", "13", "long"),
                condition("EPDSsumt1", "!=", 13, "long"),
                condition("EPDSsumt1", "=", "-2", "long"));

        String query = properties.getElasticQueryString().get();
        assertTrue(query.contains("long_data_EPDSsumt1:[\"13\" TO *]"), query);
        assertTrue(query.contains("(_exists_:long_data_EPDSsumt1 AND NOT long_data_EPDSsumt1:\"13\")"), query);
        assertTrue(query.contains("long_data_EPDSsumt1:\"-2\""), query);
        assertFalse(query.contains("data_EPDSsumt1:>="), query);
        assertEquals(List.of(new RuntimeFieldSpec("long_data_EPDSsumt1", "data_EPDSsumt1", RuntimeFieldKind.LONG)),
                properties.getRuntimeFields());
    }

    @Test
    void doubleConditionsUseDoubleRuntimeField() {
        ScheduledDatacheckTriggerProperties properties = properties(
                condition("score", ">=", "13", "double"),
                condition("score", "<", "-2.5", "double"));

        String query = properties.getElasticQueryString().get();
        assertTrue(query.contains("double_data_score:[\"13.0\" TO *]"), query);
        assertTrue(query.contains("double_data_score:{* TO \"-2.5\"}"), query);
        assertEquals(List.of(new RuntimeFieldSpec("double_data_score", "data_score", RuntimeFieldKind.DOUBLE)),
                properties.getRuntimeFields());
    }

    @Test
    void booleanConditionsUseBooleanRuntimeField() {
        ScheduledDatacheckTriggerProperties properties = properties(
                condition("submitted", "=", "true", "boolean"),
                condition("consent", "!=", true, "boolean"));

        String query = properties.getElasticQueryString().get();
        assertTrue(query.contains("boolean_data_submitted:\"true\""), query);
        assertTrue(query.contains("(_exists_:boolean_data_consent AND NOT boolean_data_consent:\"true\")"), query);
        assertEquals(Set.of(
                        new RuntimeFieldSpec("boolean_data_submitted", "data_submitted", RuntimeFieldKind.BOOLEAN),
                        new RuntimeFieldSpec("boolean_data_consent", "data_consent", RuntimeFieldKind.BOOLEAN)),
                Set.copyOf(properties.getRuntimeFields()));
    }

    @Test
    void stringConditionsMatchExactlyOnKeywordField() {
        ScheduledDatacheckTriggerProperties properties = properties(
                condition("seed", "!=", "abc", "string"),
                condition("seed", "=", "xyz", "string"),
                condition("grade", "<=", "c", "string"),
                condition("stars", "=", "***", "string"),
                condition("strength", "=", "very strong", "string"),
                condition("wild", "=", "a*b?", "string"));

        String query = properties.getElasticQueryString().get();
        assertTrue(query.contains("(_exists_:data_seed.keyword AND NOT data_seed.keyword:\"abc\")"), query);
        assertTrue(query.contains("data_seed.keyword:\"xyz\""), query);
        assertTrue(query.contains("data_grade.keyword:[* TO \"c\"]"), query);
        assertTrue(query.contains("data_stars.keyword:\"***\""), query);
        assertTrue(query.contains("data_strength.keyword:\"very strong\""), query);
        assertTrue(query.contains("data_wild.keyword:\"a*b?\""), query);
        assertTrue(properties.getRuntimeFields().isEmpty());
    }

    @Test
    void valuesCannotBreakOutOfQuotedLiteral() {
        ScheduledDatacheckTriggerProperties properties = properties(
                condition("s", "=", "a\") OR (*", "string"),
                condition("s", "=", "\\\" OR *", "string"),
                condition("l", "=", "a\") OR (*", null),
                condition("l", ">", "5) OR (*", null));

        String query = properties.getElasticQueryString().get();
        assertTrue(query.contains("data_s.keyword:\"a\\\") OR (*\""), query);
        assertTrue(query.contains("data_s.keyword:\"\\\\\\\" OR *\""), query);
        assertTrue(query.contains("data_l:\"a\\\") OR (*\""), query);
        assertTrue(query.contains("data_l:>5\\)\\ OR\\ \\(\\*"), query);
    }

    @Test
    void quoteEscapesBackslashBeforeQuote() {
        assertEquals("\"***\"", DataPointQuery.quote("***"));
        assertEquals("\"say \\\"hi\\\"\"", DataPointQuery.quote("say \"hi\""));
        assertEquals("\"a\\\\\\\"b\"", DataPointQuery.quote("a\\\"b"));
    }

    @Test
    void longAndDoubleOnSamePropertyGetSeparateRuntimeFields() {
        ScheduledDatacheckTriggerProperties properties = properties(
                condition("score", "=", "5", "long"),
                condition("score", ">", "5.5", "double"),
                condition("score", "<", "9", "long"));

        List<RuntimeFieldSpec> runtimeFields = properties.getRuntimeFields();
        assertEquals(2, runtimeFields.size(), runtimeFields::toString);
        assertEquals(Set.of(
                        new RuntimeFieldSpec("long_data_score", "data_score", RuntimeFieldKind.LONG),
                        new RuntimeFieldSpec("double_data_score", "data_score", RuntimeFieldKind.DOUBLE)),
                Set.copyOf(runtimeFields));
    }

    @Test
    void fieldNamesWithBracketsAreEscaped() {
        ScheduledDatacheckTriggerProperties properties = properties(
                condition("PRAQt1[PRAQ1t1]", ">", "3", "long"),
                condition("PRAQt1[PRAQ2t1]", "=", "A1", "string"));

        String query = properties.getElasticQueryString().get();
        assertTrue(query.contains("long_data_PRAQt1\\[PRAQ1t1\\]:{\"3\" TO *}"), query);
        assertTrue(query.contains("data_PRAQt1\\[PRAQ2t1\\].keyword:\"A1\""), query);
        assertEquals(List.of(new RuntimeFieldSpec("long_data_PRAQt1[PRAQ1t1]", "data_PRAQt1[PRAQ1t1]", RuntimeFieldKind.LONG)),
                properties.getRuntimeFields());
    }

    @Test
    void validateQuery() {
        DatacheckQueryValue prop = new DatacheckQueryValue("queryObject");

        assertEquals(ValidationIssue.NONE, validate(prop, condition("x", ">", 5, null)));
        assertEquals(ValidationIssue.NONE, validate(prop, condition("x", ">=", "13", "long")));
        assertEquals(ValidationIssue.NONE, validate(prop, condition("x", "=", 13, "long")));
        assertEquals(ValidationIssue.NONE, validate(prop, condition("x", "!=", "-2", "long")));
        assertEquals(ValidationIssue.NONE, validate(prop, condition("x", ">=", "13.5", "double")));
        assertEquals(ValidationIssue.NONE, validate(prop, condition("x", "<", 2, "double")));
        assertEquals(ValidationIssue.NONE, validate(prop, condition("submitted", "=", true, "boolean")));
        assertEquals(ValidationIssue.NONE, validate(prop, condition("seed", "!=", "abc", "string")));
        assertEquals(ValidationIssue.NONE, validate(prop, condition("x", ">", "a", "string")));

        assertEquals(ValidationIssue.Type.ERROR, validate(prop, condition("x", ">=", "13.5", "long")).getType());
        assertEquals(ValidationIssue.Type.ERROR, validate(prop, condition("x", ">=", "", "long")).getType());
        assertEquals(ValidationIssue.Type.ERROR, validate(prop, condition("x", "=", "13.5", "double")).getType());
        assertEquals(ValidationIssue.Type.ERROR, validate(prop, condition("x", "!=", "13.5", "double")).getType());
        assertEquals(ValidationIssue.Type.ERROR, validate(prop, condition("x", ">=", "13,5", "double")).getType());
        assertEquals(ValidationIssue.Type.ERROR, validate(prop, condition("x", ">", "true", "boolean")).getType());
        assertEquals(ValidationIssue.Type.ERROR, validate(prop, condition("x", "=", "yes", "boolean")).getType());
        assertEquals(ValidationIssue.Type.ERROR, validate(prop, condition("x", ">=", "13", "number")).getType());
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
