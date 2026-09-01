/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache License, Version 2.0.
 */
package io.redlink.more.studymanager.core.properties.model;

import io.redlink.more.studymanager.core.validation.ValidationIssue;

public class NamedIntegerRangeValue extends Value<NamedIntegerRange> {

    private int min = 0;
    private int max = Integer.MAX_VALUE;

    public NamedIntegerRangeValue(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "INTEGER_RANGE";
    }

    @Override
    public Class<NamedIntegerRange> getValueType() {
        return NamedIntegerRange.class;
    }

    @Override
    public ValidationIssue doValidate(NamedIntegerRange namedRange) {
        if (namedRange != null && namedRange.getName() == null || namedRange.getName().isBlank()) {
            return ValidationIssue.error(this, "The name of the RangeValue MUST NOT be blank");
        }
        if (namedRange != null && namedRange.getLower() > namedRange.getUpper()) {
            return ValidationIssue.error(this, "Lower bound of RangeValue MUST NOT be higer as the upper bound (lower:" + namedRange.getLower() + ",  upper: " + namedRange.getUpper() + ")");
        }
        if (namedRange != null && (namedRange.getLower() < getMin() || namedRange.getUpper() > getMax())) {
            return ValidationIssue.error(this, "Value must between " + getMin() + " and " + getMax());
        }
        return ValidationIssue.NONE;
    }

    public int getMin() {
        return min;
    }

    public NamedIntegerRangeValue setMin(int min) {
        this.min = min;
        return this;
    }

    public int getMax() {
        return max;
    }

    public NamedIntegerRangeValue setMax(int max) {
        this.max = max;
        return this;
    }

    @Override
    public Value<NamedIntegerRange> clone() {
        return copyState(new NamedIntegerRangeValue(getId())
                .setMax(getMax())
                .setMin(getMin()));
    }

}
