/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Elastic License 2.0.
 */
package io.redlink.more.studymanager.core.properties.model;

/**
 * Marker for a Value group consisting out of all Values where the ID
 * starts with '<code>${valuegroup.id}.</code>`.
 *
 * Example:
 * <code>
 *     Value&lt;Void&gt; group = new ValueGroup("group");
 *     Value&lt;Integer&gt; value = new InteverValue(group.getId() + ".value");
 *     Value&lt;String&gt; unit = new StringValue(group.getId() + ".unit");
 * </code>
 *
 * This will place the value and unit into the same row ordered from left to right
 * based on the order of the values in the property list. It does not contribute to the
 * configuration.
 *
 * The ValueGroup property is always:
 * <ul>
 *     <li>{@link Value#isImmutable()} == true</li>
 *     <li>{@link Value#getDefaultValue()} == null</li>
 *     <li>{@link Value#isRequired()} == false</li>
 * </ul>
 */
public class ValueGroup extends Value<Void> {
    public ValueGroup(String id) {
        super(id);
    }

    @Override
    public Class<Void> getValueType() {
        return Void.class;
    }

    @Override
    public String getType() {
        return "GROUPING";
    }


    @Override
    public final boolean isImmutable() {
        return true;
    }

    @Override
    public final Void getDefaultValue() {
        return null;
    }

    @Override
    public final boolean isRequired() {
        return false;
    }

}
