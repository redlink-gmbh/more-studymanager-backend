/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.core.properties.model;

import io.redlink.more.studymanager.core.validation.ValidationIssue;

public class IntegerRangeValue extends Value<IntegerRange> {

    // Defines the minimum value of the range, not equal to IntegerRange.lower which defines the user set lower bound
    private int min = 0;
    // Defines the maximum value of the range, not equal to IntegerRange.upper which defines the user set upper bound
    private int max = Integer.MAX_VALUE;

    public IntegerRangeValue(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "INTEGER_RANGE";
    }

    @Override
    public Class<IntegerRange> getValueType() {
        return IntegerRange.class;
    }

    @Override
    public ValidationIssue doValidate(IntegerRange range) {
        if (range != null && range.getLower() > range.getUpper()) {
            return ValidationIssue.error(this, "Lower bound of RangeValue MUST NOT be higer as the upper bound (lower:" + range.getLower() + ",  upper: " + range.getUpper() + ")");
        }
        if (range != null && (range.getLower() < getMin() || range.getUpper() > getMax())) {
            return ValidationIssue.error(this, "Value must between " + getMin() + " and " + getMax());
        }
        return ValidationIssue.NONE;
    }

    public int getMin() {
        return min;
    }

    public IntegerRangeValue setMin(int min) {
        this.min = min;
        return this;
    }

    public int getMax() {
        return max;
    }

    public IntegerRangeValue setMax(int max) {
        this.max = max;
        return this;
    }

}
