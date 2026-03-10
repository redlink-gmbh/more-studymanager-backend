/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.core.properties.model;

public class RangeSelectorValue extends Value<RangeSelectorValue.RangeSelector> {
    public RangeSelectorValue(String id) {
        super(id);
    }

    @Override
    public Class<RangeSelector> getValueType() {
        return RangeSelector.class;
    }

    @Override
    public String getType() {
        return "RANGE_SELECTOR";
    }

    public record RangeSelector(
        String label,
        String valueType,
        Number minValue,
        Number maxValue
    ){}

}
