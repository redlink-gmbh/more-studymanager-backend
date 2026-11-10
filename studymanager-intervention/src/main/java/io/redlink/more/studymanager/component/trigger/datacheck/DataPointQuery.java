/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.component.trigger.datacheck;

import io.redlink.more.studymanager.core.io.RuntimeFieldKind;
import io.redlink.more.studymanager.core.io.RuntimeFieldSpec;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

public class DataPointQuery {

    public static final String TYPE_STRING = "string";
    public static final String TYPE_LONG = "long";
    public static final String TYPE_DOUBLE = "double";
    public static final String TYPE_BOOLEAN = "boolean";

    static final Set<String> EQUALITY_OPERATORS = Set.of("=", "==", "!=");
    // equality on doubles is unreliable, so double only allows ranges
    static final Set<String> RANGE_OPERATORS = Set.of("<", ">", "<=", ">=");
    static final Set<String> COMPARISON_OPERATORS = Set.of("=", "==", "!=", "<", ">", "<=", ">=");

    private static final String QUERY_STRING_RESERVED = "+-=&|><!(){}[]^\"~*?:\\/ ";

    private int observationId;
    private String observationType;
    private String observationProperty;
    private String operator;
    private Object propertyValue;
    /**
     * How the value is compared: string, long, double or boolean. {@code null} (triggers created before this property
     * existed) keeps the original behaviour.
     */
    private String queryDataType;

    public DataPointQuery() {
    }

    public int getObservationId() {
        return observationId;
    }

    public String getObservationType() {
        return observationType;
    }

    public String getObservationProperty() {
        return observationProperty;
    }

    public String getOperator() {
        return operator;
    }

    public Object getPropertyValue() {
        return propertyValue;
    }

    public String getQueryDataType() {
        return queryDataType;
    }

    public String toQueryString() {
        return "(observation_id.keyword:" + observationId +
                " AND " + getDataSelector() + ")";
    }

    /**
     * The runtime field (name &rarr; source field) this condition needs, if any.
     */
    public Optional<RuntimeFieldSpec> getRuntimeField() {
        return runtimeFieldKind().map(kind -> new RuntimeFieldSpec(
                kind.name().toLowerCase() + "_" + dataFieldName(), dataFieldName(), kind));
    }

    private Optional<RuntimeFieldKind> runtimeFieldKind() {
        if (queryDataType == null) {
            return Optional.empty();
        }
        return switch (queryDataType) {
            case TYPE_LONG -> Optional.of(RuntimeFieldKind.LONG);
            case TYPE_DOUBLE -> Optional.of(RuntimeFieldKind.DOUBLE);
            case TYPE_BOOLEAN -> Optional.of(RuntimeFieldKind.BOOLEAN);
            default -> Optional.empty();
        };
    }

    /**
     * @return an error message if this condition is invalid for its data type, otherwise empty
     */
    public Optional<String> validate() {
        if (queryDataType == null) {
            return Optional.empty();
        }
        return switch (queryDataType) {
            case TYPE_STRING -> EQUALITY_OPERATORS.contains(operator) ? Optional.empty()
                    : Optional.of("Operator '" + operator + "' is not allowed for data type string");
            case TYPE_BOOLEAN -> {
                if (!EQUALITY_OPERATORS.contains(operator)) {
                    yield Optional.of("Operator '" + operator + "' is not allowed for data type boolean");
                }
                yield parseBoolean().isPresent() ? Optional.empty()
                        : Optional.of("Value '" + propertyValue + "' is not a boolean (true/false)");
            }
            case TYPE_LONG -> {
                if (!COMPARISON_OPERATORS.contains(operator)) {
                    yield Optional.of("Operator '" + operator + "' is not allowed for data type long");
                }
                yield parseLong().isPresent() ? Optional.empty()
                        : Optional.of("Value '" + propertyValue + "' is not a whole number");
            }
            case TYPE_DOUBLE -> {
                if (!RANGE_OPERATORS.contains(operator)) {
                    yield Optional.of("Operator '" + operator + "' is not allowed for data type double");
                }
                yield parseDouble().isPresent() ? Optional.empty()
                        : Optional.of("Value '" + propertyValue + "' is not a valid number");
            }
            default -> Optional.of("Unknown data type '" + queryDataType + "'");
        };
    }

    private String getDataSelector() {
        Optional<RuntimeFieldSpec> runtimeField = getRuntimeField();
        if (runtimeField.isPresent()) {
            return getRuntimeFieldSelector(runtimeField.get());
        }
        String field = escape(dataFieldName());
        if ("=".equals(operator) || "==".equals(operator)) {
            return field + ":" + getSanitizedPropertyValue();
        } else if ("!=".equals(operator)) {
            return "NOT " + field + ":" + getSanitizedPropertyValue();
        } else if ("<".equals(operator) || ">".equals(operator) || "<=".equals(operator) || ">=".equals(operator)) {
            return field + ":" + operator + propertyValue;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private String getRuntimeFieldSelector(RuntimeFieldSpec runtimeField) {
        String field = escape(runtimeField.target());
        Optional<String> parsed = switch (runtimeField.kind()) {
            case LONG -> parseLong().map(String::valueOf);
            case DOUBLE -> parseDouble().map(BigDecimal::toPlainString);
            case BOOLEAN -> parseBoolean().map(String::valueOf);
        };
        String value = "\"" + parsed.orElseThrow(() -> new IllegalArgumentException(
                "Value '" + propertyValue + "' is not a valid " + queryDataType)) + "\"";
        return switch (operator) {
            case "=", "==" -> field + ":" + value;
            // _exists_ guard: documents without a readable value (e.g. empty LimeSurvey responses) must not match
            case "!=" -> "(_exists_:" + field + " AND NOT " + field + ":" + value + ")";
            case "<" -> field + ":{* TO " + value + "}";
            case "<=" -> field + ":[* TO " + value + "]";
            case ">" -> field + ":{" + value + " TO *}";
            case ">=" -> field + ":[" + value + " TO *]";
            default -> throw new UnsupportedOperationException();
        };
    }

    private Optional<BigDecimal> parseDouble() {
        if (propertyValue == null) {
            return Optional.empty();
        }
        try {
            double d = Double.parseDouble(String.valueOf(propertyValue).trim());
            return Double.isFinite(d) ? Optional.of(BigDecimal.valueOf(d)) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<Long> parseLong() {
        if (propertyValue == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(String.valueOf(propertyValue).trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<Boolean> parseBoolean() {
        String v = String.valueOf(propertyValue).trim();
        return "true".equals(v) || "false".equals(v) ? Optional.of(Boolean.valueOf(v)) : Optional.empty();
    }

    private String dataFieldName() {
        return "data_" + observationProperty;
    }

    static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (QUERY_STRING_RESERVED.indexOf(c) >= 0) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private Object getSanitizedPropertyValue() {
        if (this.propertyValue instanceof String p) {
            return p.contains(" ") && !p.contains("\"") ?
                    "\"" + p.trim() + "\"" :
                    p.trim();
        }
        return this.propertyValue;
    }
}
